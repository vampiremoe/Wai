# Keep our custom classes
-keep class com.wai.vaultapp.** { *; }

# Keep file provider
-keep class androidx.core.content.FileProvider

# Keep material components
-keep class com.google.android.material.** { *; }

# Keep AndroidX components
-keep class androidx.** { *; }

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
