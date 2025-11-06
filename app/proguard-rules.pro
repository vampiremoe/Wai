# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep our application classes
-keep class com.wai.vaultapp.** { *; }

# Keep AndroidX components
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep view binding
-keep class * extends androidx.viewbinding.ViewBinding { *; }

# Keep parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep serializable
-keepclassmembers class * implements java.io.Serializable {
  static final long serialVersionUID;
  private static final java.io.ObjectStreamField[] serialPersistentFields;
  private void writeObject(java.io.ObjectOutputStream);
  private void readObject(java.io.ObjectInputStream);
  java.lang.Object writeReplace();
  java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep activities
-keep public class * extends android.app.Activity

# Keep application
-keep public class * extends android.app.Application

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
