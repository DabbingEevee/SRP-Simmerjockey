package com.existingeevee.simmerjockey.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;

import org.spongepowered.asm.launch.MixinInitialisationError;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;

import com.existingeevee.simmerjockey.Simmerjockey;

import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModClassLoader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mixin(value = LoadController.class, remap = false)
public class MixinLoaderLateConfigs {

	@Shadow
	private Loader loader;

	@Inject(method = "distributeStateMessage(Lnet/minecraftforge/fml/common/LoaderState;[Ljava/lang/Object;)V", at = @At("HEAD"))
	private void simmerjockey$distributeStateMessage(LoaderState state, Object[] eventData, CallbackInfo ci) throws Throwable {
		if (state == LoaderState.CONSTRUCTING) {
			ModClassLoader modClassLoader = (ModClassLoader) eventData[0];
			
			@SuppressWarnings("unchecked")
			List<ModContainer> mods = (List<ModContainer>) ReflectionHelper.findField(Loader.class, "mods", null).get(loader);

			for (ModContainer mod : mods) {
				try {
					modClassLoader.addFile(mod.getSource());
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}

			try {
				// Add and reload mixin configs
				Mixins.addConfiguration("mixins." + Simmerjockey.MODID + ".late.json");
			} catch (MixinInitialisationError e) {
				throw new IllegalStateException(Simmerjockey.MODID + " late mixins failed to apply. This is likely due to another mod using a newer version of mixins. Try installing \"Mixin 0.7-0.8 Compatibility\" by NotStirred (https://www.curseforge.com/minecraft/mc-mods/mixin-0-7-0-8-compatibility).");
			}

			try {
				// This will very likely break on the next major mixin release.
				Class<?> proxyClass = Class.forName("org.spongepowered.asm.mixin.transformer.Proxy");
				Field transformerField = proxyClass.getDeclaredField("transformer");
				transformerField.setAccessible(true);
				Object transformer = transformerField.get(null);

				Class<?> mixinTransformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
				Field processorField = mixinTransformerClass.getDeclaredField("processor");
				processorField.setAccessible(true);
				Object processor = processorField.get(transformer);

				Class<?> mixinProcessorClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinProcessor");

				Field extensionsField = mixinProcessorClass.getDeclaredField("extensions");
				extensionsField.setAccessible(true);
				Object extensions = extensionsField.get(processor);

				Method selectConfigsMethod = mixinProcessorClass.getDeclaredMethod("selectConfigs", MixinEnvironment.class);
				selectConfigsMethod.setAccessible(true);
				selectConfigsMethod.invoke(processor, MixinEnvironment.getCurrentEnvironment());

				// Mixin 0.8.4+
				try {
					Method prepareConfigs = mixinProcessorClass.getDeclaredMethod("prepareConfigs", MixinEnvironment.class, Extensions.class);
					prepareConfigs.setAccessible(true);
					prepareConfigs.invoke(processor, MixinEnvironment.getCurrentEnvironment(), extensions);
					return;
				} catch (NoSuchMethodException ex) {
					// no-op
				}

				// Mixin 0.8+
				try {
					Method prepareConfigs = mixinProcessorClass.getDeclaredMethod("prepareConfigs", MixinEnvironment.class);
					prepareConfigs.setAccessible(true);
					prepareConfigs.invoke(processor, MixinEnvironment.getCurrentEnvironment());
					return;
				} catch (NoSuchMethodException ex) {
					// no-op
				}

				throw new UnsupportedOperationException("Unsupported Mixin");
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}