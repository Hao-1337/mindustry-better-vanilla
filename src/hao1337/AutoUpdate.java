package hao1337;

import arc.Application;
import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.util.Http;
import arc.util.Log;
import arc.util.Http.HttpResponse;
import arc.util.io.Streams;
import arc.util.serialization.Jval;
import java.net.URLClassLoader;
import java.util.Objects;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.mod.Mods.LoadedMod;
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

   public static LoadedMod mod;

   public static float progress;
   public static String download;

   private static Fi modFile;

   public static void load(String InputUrl, String InputPackname, String InputRepo, String InputUnzipName, String Version) {
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
         }
         else try {
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
         
         // OMG i'm stupid LMAOOOO
         int ver = Integer.parseInt(meta.getString("version").replace(".", ""));
         if (ver > modBuild) modBuild = ver;
      } catch (Throwable err) {
        Log.err(err);
      }
   }

   public static void check() {
      Log.info("Checking for updates at: " + url);
      Http.get(url, (res) -> {
         Jval json = Jval.read(res.getResultAsString());
         latest = json.getString("tag_name").substring(1);
         download = ((Jval)json.get("assets").asArray().get(0)).getString("browser_download_url");
         latestBuild = Integer.parseInt(json.getString("tag_name").substring(1).replace(".", ""));
         Log.info("Fetch complete, version: [accent]" + latestBuild + "[]. User version: [accent]" + modBuild + "[](" + mod.meta.version + ")");
         if (latestBuild > modBuild) {
            Vars.ui.showCustomConfirm(Core.bundle.format("hao1337.update.name"), Core.bundle.format("hao1337.update.info", new Object[]{versionString, latest}), Core.bundle.format("hao1337.update.ok"), Core.bundle.format("hao1337.update.nope"), AutoUpdate::update, () -> {
            });
         }

      }, Log::err);
   }

   public static void update() {
      try {
         ClassLoader var1 = mod.loader;
         if (var1 instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader)var1;
            cl.close();
         }

         mod.loader = null;
      } catch (Throwable var2) {
         Log.err(var2);
      }

      Vars.ui.loadfrag.show(Core.bundle.format("hao1337.update.updating"));
      Vars.ui.loadfrag.setProgress(() -> {
         return progress;
      });
      Http.get(download, AutoUpdate::handle, Log::err);
   }

   public static void handle(HttpResponse res) {
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
         UI var3 = Vars.ui;
         Application app1 = Core.app;
         Objects.requireNonNull(app1);
         var3.showInfoOnHidden(Core.bundle.format("hao1337.reloadexit"), app1::exit);
      } catch (Throwable var2) {
         Log.err(var2);
      }

   }

   public static boolean installed(String mod) {
      return Vars.mods.getMod(mod) != null && Vars.mods.getMod(mod).enabled();
   }
}
