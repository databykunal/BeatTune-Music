# Sonify release rules
# Keep empty unless app-specific R8 rules are required.

# NewPipe Extractor (used for direct YouTube extraction)
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter { *; }
-dontwarn org.mozilla.javascript.tools.**
