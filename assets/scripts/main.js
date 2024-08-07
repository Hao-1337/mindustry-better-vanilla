const urlLoader = Packages.java.net.URLClassLoader([ Vars.mods.getMod(modName).file.file().toURI().toURL() ], Vars.mods.mainLoader());

function getClass(name) {
	return Packages.rhino.NativeJavaClass(Vars.mods.scripts.scope, urlLoader.loadClass(name));
};

const MultiBlockMachineC = getClass("hao1337.lib.MultiBlockMachine");

modules.exports = {
    MultiBlockMachine: MultiBlockMachineC
}