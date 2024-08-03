package hao1337;

import arc.Application;
import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
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

import java.net.URLClassLoader;
import java.util.Objects;

import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.fragments.LoadingFragment;

public class AutoUpdate {
	public static String repo;
	public static String packname;
	public static String unzipName;
	public static String url;
	public static int latestBuild;
	private static String latest;
	private static int modBuild;
	private static String versionString;
	private static Seq<Jval> versions;

	public static LoadedMod mod;

	public static float progress;
	public static String download;

	private static Fi modFile;

	public static void load(String InputUrl, String InputPackname, String InputRepo, String InputUnzipName,
			String Version) {
		try {
			packname = InputPackname;
			repo = InputRepo;
			url = InputUrl;
			unzipName = InputUnzipName;
			modBuild = Integer.parseInt(Version.replace(".", ""));
			versionString = Version;

			mod = Vars.mods.getMod(packname);

			if (mod == null) {
				modFile = Vars.modDirectory.child(unzipName);
				if (!modFile.exists()) {
					Log.err("Mod file not found: " + packname);
					return;
				}
			} else
				try {
					modFile = new ZipFi(mod.file);
				} catch (Throwable e) {
					modFile = Vars.modDirectory.child(unzipName);
					if (!modFile.exists()) {
						Log.err("Mod file not found: " + packname);
						return;
					}
				}

			Jval meta = Jval.read(modFile.child("mod.hjson").readString());
			mod.meta.author = meta.getString("author");
			mod.meta.description = meta.getString("description");

			int ver = Integer.parseInt(meta.getString("version").replace(".", ""));
			if (ver > modBuild)
				modBuild = ver;
		} catch (Throwable err) {
			Log.err(err);
		}
	}

	public static void check() {
		Log.info("Checking for updates at: " + url);

		Http.get(url, (res) -> {
			versions = ((Seq<Jval>) Jval.read(res.getResultAsString()).asArray());

			Jval json = versions.get(0);

			latest = json.getString("tag_name").substring(1);
			download = ((Jval) json.get("assets").asArray().get(0)).getString("browser_download_url");
			latestBuild = Integer.parseInt(json.getString("tag_name").substring(1).replace(".", ""));

			Log.info("Fetch complete, version: [accent]" + latestBuild + "[]. User version: [accent]" + modBuild + "[]("
					+ mod.meta.version + ")");

			if (latestBuild > modBuild)
				showCustomConfirm(latest, versionString);
		}, Log::err);
	}

	private static void showCustomConfirm(String lastestVer, String currentVer) {
		BaseDialog dialog = new BaseDialog(Core.bundle.format("hao1337.update.name"));
		dialog.setFillParent(true);
		dialog.cont.top().left().margin(20f);

		if (versions.size <= 0) {
			Log.err("WTF just happen?");
			return;
		}

		float maxWidth = Math.max(Core.graphics.getWidth() / Scl.scl(1.08f), 520f);
		Table content = dialog.cont;

		content.add(Core.bundle.format("hao1337.update.info", new Object[] { lastestVer, currentVer })).fillX().padLeft(80f).padRight(80f);
		content.row();

		content.table(t -> {
			ScrollPane pane = t.pane(desc -> {
				desc.center().defaults().padTop(10).left();
				String lastVer = versionString;

				for (Jval json : versions.reverse()) {
					String info = json.getString("body"),
							version = json.getString("tag_name");

					if (version.contains("beta"))
						continue;

					int buildVer = Integer.parseInt(version.substring(1).replace(".", ""));

					if (buildVer < modBuild)
						continue;

					desc.add(Core.bundle.format("hao1337.update.changelog", new Object[] { lastVer, version }));
					desc.row();
					desc.add("[sky]" + info).wrap().padLeft(30f).padTop(-8f).growX();
					desc.row();
				}
			}).width(maxWidth).padRight(10f).get();

			pane.setScrollingDisabledX(false);
			pane.setStyle(Styles.noBarPane);
		}).padLeft(30f).padTop(5f);

		dialog.cont.fill(t -> {
			t.bottom().label(() ->  Core.bundle.format("hao1337.update.confirm")).fillX().center();
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
			if (loader instanceof URLClassLoader) {
				URLClassLoader cl = (URLClassLoader) loader;
				cl.close();
			}

			mod.loader = null;
		} catch (Throwable err) {
			Log.err(err);
			Vars.ui.showException(err);
		}

		Vars.ui.loadfrag.show(Core.bundle.format("hao1337.update.updating"));
		Vars.ui.loadfrag.setProgress(() -> {
			return progress;
		});
		Http.get(download, AutoUpdate::handle, Log::err);
	}

	private static void handle(HttpResponse res) {
		try {
			Fi file = Vars.tmpDirectory.child(repo.replace("/", "") + ".zip");
			Streams.copyProgress(res.getResultAsStream(), file.write(false), res.getContentLength(), 4096, (p) -> {
				progress = p;
			});
			Vars.mods.importMod(file).setRepo(repo);
			file.delete();
			Application app = Core.app;
			LoadingFragment fragment = Vars.ui.loadfrag;
			Objects.requireNonNull(fragment);
			app.post(fragment::hide);
			Application app1 = Core.app;
			Objects.requireNonNull(app1);
			Vars.ui.showInfoOnHidden(Core.bundle.format("hao1337.reloadexit"), app1::exit);
		} catch (Throwable err) {
			Log.err(err);
		}

	}

	public static boolean installed(String mod) {
		return Vars.mods.getMod(mod) != null && Vars.mods.getMod(mod).enabled();
	}
}
