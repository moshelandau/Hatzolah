# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data classes for serialization
-keepclassmembers class com.hatzolah.app.data.database.entity.** { *; }
