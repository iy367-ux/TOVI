const OWNER = "iy367-ux";
const REPO = "TOVI";
const RELEASES_API = `https://api.github.com/repos/${OWNER}/${REPO}/releases`;
const RELEASES_PAGE = `https://github.com/${OWNER}/${REPO}/releases`;
const $ = (id) => document.getElementById(id);

function formatDate(value) {
  if (!value) return "Date unavailable";
  return new Intl.DateTimeFormat(undefined, { day: "numeric", month: "short", year: "numeric" }).format(new Date(value));
}
function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return "";
  const units = ["B", "KB", "MB", "GB"];
  let size = bytes, index = 0;
  while (size >= 1024 && index < units.length - 1) { size /= 1024; index++; }
  return `${size.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}
function escapeHtml(value = "") {
  return String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}
function apkAssets(release) {
  return (release.assets || []).filter(asset => asset.name.toLowerCase().endsWith(".apk"));
}
function renderUnavailable(message) {
  $("release-status-text").textContent = "No stable release available yet";
  $("release-title").textContent = "TOVI is getting ready.";
  $("release-description").textContent = message || "There isn't a published stable Android release yet. When one is published to GitHub Releases, it will appear here automatically.";
  $("release-meta").innerHTML = "";
  $("release-action").innerHTML = '<a class="download-button" href="' + RELEASES_PAGE + '" target="_blank" rel="noopener">Follow development ↗</a>';
}
function renderLatest(release) {
  const apks = apkAssets(release);
  $("release-status-text").textContent = release.prerelease ? "Latest preview release" : "Latest stable release";
  $("release-title").textContent = release.name || release.tag_name || "TOVI for Android";
  const notes = (release.body || "").trim();
  $("release-description").textContent = notes ? notes.replace(/[#>*_\x60]/g, "").replace(/\s+/g, " ").slice(0, 220) : "Official TOVI Android release.";
  const apk = apks[0];
  const meta = [release.tag_name, formatDate(release.published_at), apk ? formatBytes(apk.size) : ""].filter(Boolean);
  $("release-meta").innerHTML = meta.map(item => `<span>${escapeHtml(item)}</span>`).join("");
  if (apk) {
    $("release-action").innerHTML = `<a class="download-button" href="${escapeHtml(apk.browser_download_url)}">Download APK ↓</a>`;
  } else {
    $("release-action").innerHTML = `<a class="download-button" href="${escapeHtml(release.html_url)}" target="_blank" rel="noopener">View release ↗</a>`;
    $("release-description").textContent += " No APK asset is attached to this release yet.";
  }
}
function renderHistory(releases) {
  const list = $("release-history-list");
  if (!releases.length) {
    list.innerHTML = '<p class="muted">Release history will appear here when the first version is published.</p>';
    return;
  }
  list.innerHTML = releases.slice(0, 5).map(release => {
    const apks = apkAssets(release);
    const label = release.prerelease ? "Preview" : "Stable";
    return `<div class="history-item"><div><strong>${escapeHtml(release.name || release.tag_name)}</strong><small>${escapeHtml(release.tag_name)} · ${formatDate(release.published_at)} · ${label}</small></div><a href="${escapeHtml(apks[0]?.browser_download_url || release.html_url)}" target="_blank" rel="noopener">${apks.length ? "Download ↓" : "Release notes ↗"}</a></div>`;
  }).join("");
}
async function loadReleases() {
  const button = $("refresh-releases");
  button.disabled = true;
  $("release-status-text").textContent = "Checking official releases…";
  try {
    const response = await fetch(`${RELEASES_API}?per_page=6`, {
      headers: { Accept: "application/vnd.github+json" },
      cache: "no-store"
    });
    if (!response.ok) throw new Error(response.status === 404 ? "No releases have been published yet." : `GitHub returned ${response.status}. Please try again shortly.`);
    const releases = await response.json();
    const stable = releases.find(release => !release.draft && !release.prerelease);
    const latestPreview = releases.find(release => !release.draft && release.prerelease);
    if (stable) renderLatest(stable);
    else if (latestPreview) renderLatest(latestPreview);
    else renderUnavailable("The official release feed is connected. The first TOVI release will appear here as soon as it is published.");
    renderHistory(releases.filter(release => !release.draft));
  } catch (error) {
    renderUnavailable(`${error.message} You can always visit the official GitHub Releases page.`);
    $("release-history-list").innerHTML = `<p class="muted">Release history is temporarily unavailable. <a href="${RELEASES_PAGE}" target="_blank" rel="noopener">Open GitHub Releases ↗</a></p>`;
  } finally {
    button.disabled = false;
  }
}
const menuButton = document.querySelector(".menu");
const nav = $("site-nav");
menuButton?.addEventListener("click", () => {
  const open = nav.classList.toggle("open");
  menuButton.setAttribute("aria-expanded", String(open));
  menuButton.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
});
nav?.querySelectorAll("a").forEach(link => link.addEventListener("click", () => {
  nav.classList.remove("open");
  menuButton?.setAttribute("aria-expanded", "false");
}));
$("year").textContent = new Date().getFullYear();
$("refresh-releases").addEventListener("click", loadReleases);
loadReleases();
window.setInterval(loadReleases, 5 * 60 * 1000);