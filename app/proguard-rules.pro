# NuBook Proguard Rules
# 保持 Room 实体和 DAO
-keep class com.nubook.data.local.entity.** { *; }
-keep class com.nubook.data.local.dao.** { *; }

# 保持 mXparser
-keep class org.mariuszgromada.math.mxparser.** { *; }

# 保持 Gson 序列化类
-keepattributes Signature
-keepattributes *Annotation*
