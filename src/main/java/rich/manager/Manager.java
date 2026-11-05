package rich.manager;
public final class Manager {
 private final rich.util.render.shader.RenderCore renderCore = new rich.util.render.shader.RenderCore();
 private final rich.modules.module.ModuleRepository moduleRepository = new rich.modules.module.ModuleRepository();
 public rich.util.render.shader.RenderCore getRenderCore() { return renderCore; }
 public rich.modules.module.ModuleRepository getModuleRepository() { return moduleRepository; }
}
