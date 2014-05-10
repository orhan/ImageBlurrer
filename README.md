ImageBlurrer (for Android)
=========================

Little nifty helper library to apply a blur-effect to a `Bitmap`. It also lets you overlay the resulting bitmap with a `Color`, `Drawable` or `Resource`. 

Setup
=====

Note: this is the setup routine for Eclipse/ANT.

1. Import `ImageBlurrer` as a new Android Project or download the latest .jar file from [here](https://github.com/orhnsnmz/ImageBlurrer/releases) and add it to your build path.
2. Add the following lines to your project.properties file (this references the [RenderScript support library](http://android-developers.blogspot.de/2013/09/renderscript-in-android-support-library.html)):
```
renderscript.target=19
enderscript.support.mode=true
sdk.buildtools=19.0.1* 

* Replace this with the most recent version of the build tools
```

Usage
=====

Using `ImageBlurrer` is straight-forward, just pass it the bitmap you would like to get blurred and you're ready to go.

Explanation of all parameters:
- `blurRadius`: Defines the blur radius to apply to the incoming bitmap. Range between 1f and 25f.
- `sampleSize`: Identical to BitmapFactory.inSampleSize. Must be >= 1. Higher values improve blurriness and performance significantly, but may result in an image that is "too washed out".
- `overlayColor`: Lets you overlay the resulting image with a `Color`. `ARGB` values should be used here, such as `#80000000` for 50% transparent black overlay.
- `overlayResource`: Lets you overlay the resulting image with a `Resource` from your `drawable` folders.
- `overlayDrawable`: Lets you overlay the resulting image with a `Drawable`.
- `overlayOpacity`: Sets the opacity of the overlaid `Resource` or `Drawable`. Ranges between `0f` for fully a transparent or `1f` for a fully opaque overlay. Something inbetween is recommended.

Note, that the combination of `blurRadius` and `sampleSize` make up for noticeable differences in performance, but as well as quality of the resulting image.

---

Code example:

```
/* 
 *	Signature of this method: 
 *	public static Bitmap ImageBlurrer.blurImage(Context context, Bitmap bitmap, float blurRadius, float sampleSize)
 *
 *	This
 */
 
Bitmap blurredBitmap = ImageBlurrer.blurImage(getApplicationContext(), originalBitmap, 12.5f);
```

To-do
=====

- Possibility to define whether the overlaying `Resource` or `Drawable` should be tiled or stretched across the bitmap.