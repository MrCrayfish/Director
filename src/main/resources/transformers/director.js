function initializeCoreMod() {
	return {
	    'transformer': {
	        'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.IngameGui',
                'methodName': 'func_212913_a',
                'methodDesc': '(Lnet/minecraft/util/math/RayTraceResult;)Z'
            },
            'transformer': function(method) {
                log("Patching IngameGui#isTargetNamedMenuProvider...");
                patch_IngameGui_isTargetNamedMenuProvider(method);
                return method;
            }
	    }
	};
}

function log(s) {
    print("[director.js] " + s);
}

var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');

function patch_IngameGui_isTargetNamedMenuProvider(method) {
    var firstNode = method.instructions.get(0);
    var jumpNode = new LabelNode();
    method.instructions.insertBefore(firstNode, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/director/path/PathManager", "isLookingAtPathPoint", "()Z", false));
    method.instructions.insertBefore(firstNode, new JumpInsnNode(Opcodes.IFEQ, jumpNode));
    method.instructions.insertBefore(firstNode, new InsnNode(Opcodes.ICONST_1));
    method.instructions.insertBefore(firstNode, new InsnNode(Opcodes.IRETURN));
    method.instructions.insertBefore(firstNode, jumpNode);
    method.instructions.insertBefore(firstNode, new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
}