package hao1337.addins;

import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Http.HttpResponse;
import arc.util.io.Streams;
import arc.util.serialization.Jval;
import hao1337.Version;
import hao1337.Version.InvalidVersionStringException;
import hao1337.Version.GameVersion;

import java.net.URLClassLoader;
import java.util.Objects;

import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static hao1337.Version.*;

/**
 * AutoUpdate class handles automatic mod updates for Mindustry mods.
 * 
 * This class manages checking for new versions of a mod from a remote
 * repository,
 * displaying update information to the user, and downloading/installing
 * updates.
 * 
 * @author Hao-1337
 */
public class AutoUpdate {
	private static Seq<Jval> versions = new Seq<>();
	private static Fi modFile;

	public static LoadedMod mod;
	public static float progress;
	public static String downloadURL;
	private static long downloadCount = 0;
	private static boolean recuseMode = false;

	private static String latestVersionString;
	private static Jval lastestAssets;
	private static String currentVersionString = Version.getVersionString();
	private static boolean allowPrerelease;
	private static boolean allowAutoUpdate;

	/**
	 * Revert to last version that accpet as the last not buggy version
	 * 
	 * @return
	 */
	public static String recuseUpdate() {
		if (Version.gameVersion == GameVersion.V157 || mindustry.core.Version.build > 147)
			return "https://github.com/Hao-1337/mindustry-better-vanilla/releases/download/v1.8.72/mindustry-better-vanilla.jar";
		return "https://github.com/Hao-1337/mindustry-better-vanilla/releases/download/v1.8.72/steam-mindustry-better-vanilla.jar";
	}

	public static void load() {
		try {
			allowPrerelease = Core.settings.getBool("hao1337.toggle.autoupdate.prerelease")
					|| Version.tag == Version.Tag.BETA || Version.tag == Version.Tag.ALPHA;
			allowAutoUpdate = Core.settings.getBool("hao1337.toggle.autoupdate");
			mod = Vars.mods.getMod(name);

			if (mod == null) {
				modFile = Vars.modDirectory.child(unzipName);
				if (!modFile.exists()) {
					Log.err("Mod folder not found: mods/@", unzipName);
					return;
				}
			} else {
				try {
					modFile = new ZipFi(mod.file);
				} catch (Throwable e) {
					modFile = Vars.modDirectory.child(unzipName);
					if (!modFile.exists()) {
						Log.err("Mod file not found: " + name);
						return;
					}
				}
			}

			checkUpdate();
		} catch (Throwable err) {
			Log.err(err);
		}
	}

	public static long getDownloadCount() {
		return downloadCount;
	}

	public static void checkUpdate() {
		Log.info("[@] Checking for updates at: @", name, gitapi);

		Http.get(gitapi, (res) -> {
			var allVersion = ((Seq<Jval>) Jval.read(res.getResultAsString()).asArray());

			allVersion.retainAll(version -> {
				try {
					if (!allowPrerelease && version.getBool("prerelease", false))
						return false;
					String tag = version.getString("tag_name");
					return Version.isLargerOrEqual(tag);
				} catch (InvalidVersionStringException e) {
					e.printStackTrace();
					return false;
				}
			});

			allVersion.sort((Jval a, Jval b) -> {
				try {
					return Version.compare(a.getString("tag_name"), b.getString("tag_name"));
				} catch (InvalidVersionStringException e) {
					e.printStackTrace();
					return -1;
				}
			});

			versions.clear();
			versions.addAll(allVersion);

			if (allVersion.isEmpty()) {
				Log.warn("[@] Fetching failed, this version doesn't match any release. Does this mod is custom build?",
						name);
				return;
			}
			if (allVersion.size == 1) {
				Log.info("[@] Fetching complete, your mod is up-to-date!", name);
				return;
			}

			lastestAssets = versions.peek();
			latestVersionString = lastestAssets.getString("tag_name");

			if (!allowAutoUpdate) {
				Log.info(
						"[@] Fetching complete, you mod is outdate (current is @, lastest is @), but auto update was disable!",
						name, currentVersionString, latestVersionString);
				return;
			}

			for (Jval asset : lastestAssets.get("assets").asArray()) {
				GameVersion version = GameVersion.parseGameVer(asset.getString("name"));

				if (version == Version.gameVersion) {
					downloadURL = asset.getString("browser_download_url");
					downloadCount = asset.getLong("download_count", 0);
					break;
				}
			}

			Log.info("[@] Fetching complete, current version: @, lastest version: @", name, currentVersionString,
					latestVersionString);
			Log.info("[@] Download URL (download counter: @): @", name, downloadCount, downloadURL);

			if (downloadURL == null) {
				Log.err("[@] Fail to fetch URL, enter recuse mode!", downloadURL);
				downloadURL = recuseUpdate();
				recuseMode = true;
			}

			showCustomConfirm();
		}, Log::err);
	}

	private static void showCustomConfirm() {
		BaseDialog dialog = new BaseDialog(Core.bundle.format("hao1337.update.name"));
		dialog.setFillParent(true);
		dialog.cont.top().left().margin(20f);

		float maxWidth = Math.max(Core.graphics.getWidth() / Scl.scl(1.08f), 520f);
		Table content = dialog.cont;

		if (recuseMode) content.add("RECUSE MODE!").color(Color.red).fillX().padLeft(80f).padRight(80f);
		content.row();
		content.add(
				Core.bundle.format("hao1337.update.info", new Object[] { latestVersionString, currentVersionString }))
				.fillX()
				.padLeft(80f).padRight(80f);
		content.row();

		content.table(t -> {
			ScrollPane pane = t.pane(desc -> {
				desc.center().defaults().padTop(10).left();
				String lastVer = currentVersionString;

				for (Jval json : versions.copy().reverse()) {
					String info = json.getString("body"),
							version = json.getString("tag_name");

					desc.add(Core.bundle.format("hao1337.update.changelog", new Object[] { lastVer, version }));
					desc.row();
					desc.add("[sky]" + info).wrap().padLeft(30f).padTop(-8f).growX();
					desc.row();
					lastVer = version;
				}
			}).width(maxWidth).padRight(10f).get();

			pane.setScrollingDisabledX(false);
			pane.setStyle(Styles.noBarPane);
		}).padLeft(30f).padTop(5f);

		dialog.cont.fill(t -> {
			t.bottom().label(() -> Core.bundle.format("hao1337.update.confirm")).fillX().center();
			t.row();
			t.table(t1 -> {
				t1.defaults().padLeft(60f).padRight(60f).padTop(20f);
				t1.button(Core.bundle.format("hao1337.update.nope"), () -> {
					dialog.hide();
				}).width(100f);
				t1.button(Core.bundle.format("hao1337.update.ok"), () -> {
					dialog.hide();
					update();
				}).width(100f);
			});
		});

		KeyCode key = KeyCode.escape;
		Objects.requireNonNull(dialog);

		dialog.keyDown(key, dialog::hide);
		key = KeyCode.back;
		Objects.requireNonNull(dialog);

		dialog.keyDown(key, dialog::hide);
		dialog.show();
	}

	private static void update() {
		try {
			ClassLoader loader = mod.loader;
			if (loader instanceof URLClassLoader cl)
				cl.close();

			mod.loader = null;
		} catch (Throwable err) {
			Log.err(err);
			Vars.ui.showException(err);
		}

		Vars.ui.loadfrag.show(Core.bundle.format("hao1337.update.updating"));
		Vars.ui.loadfrag.setProgress(() -> progress);
		Http.get(downloadURL, AutoUpdate::handle, Log::err);
	}

	private static void handle(HttpResponse res) {
		try {
			Fi file = Vars.tmpDirectory.child(repoName.replace("/", "").toLowerCase() + ".zip");
			Streams.copyProgress(res.getResultAsStream(), file.write(false), res.getContentLength(), 4096, (p) -> {
				progress = p;
			});
			cleanup();
			Vars.mods.importMod(file).setRepo(repoName);
			file.delete();
			Core.app.post(Objects.requireNonNull(Vars.ui.loadfrag)::hide);
			Vars.ui.showInfoOnHidden(Core.bundle.format("hao1337.reloadexit"), Objects.requireNonNull(Core.app)::exit);
		} catch (Throwable err) {
			Log.err(err);
		}
	}

	private static void cleanup() {
		try {
			Fi oldFile = Vars.modDirectory.child("mindustry-better-vanilla.zip");
			if (oldFile.exists()) oldFile.delete(); 
			Fi oldFile1 = Vars.modDirectory.child("hao1337mindustry-better-vanilla.zip");
			if (oldFile1.exists()) oldFile1.delete();
			Fi oldFile2 = Vars.modDirectory.child("hao1337-mindustry-better-vanilla.zip");
			if (oldFile2.exists()) oldFile2.delete();
			Fi oldFile3 = Vars.modDirectory.child("hao-1337-mindustry-better-vanilla.zip");
			if (oldFile3.exists()) oldFile3.delete();
			Fi oldFile4 = Vars.modDirectory.child("Hao-1337mindustry-better-vanilla.zip");
			if (oldFile4.exists()) oldFile.delete();
		} catch (Throwable e) {
			Log.err(e);
		}
	}

	public static boolean installed(String mod) {
		return Vars.mods.getMod(mod) != null && Vars.mods.getMod(mod).enabled();
	}
}
