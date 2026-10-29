-keepattributes *Annotation*

-keep class dev.fishpi.mobile.data.FishPiNative { *; }

-keepclassmembers class * {
    public void onEvent(java.lang.String);
}

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
