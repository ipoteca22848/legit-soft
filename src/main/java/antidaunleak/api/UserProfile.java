package antidaunleak.api;
public final class UserProfile {
 private static final UserProfile INSTANCE=new UserProfile();
 public static UserProfile getInstance(){return INSTANCE;}
 public String profile(String key){return key.equals("username")?net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername():"local";}
}
