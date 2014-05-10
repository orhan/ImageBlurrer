/*
 * Copyright (C) 2014 Orhan Sönmez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package app.majestylabs.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

@SuppressLint("NewApi")
public class ImageBlurrer {
	
	/**
	 * Blurs a bitmap
	 * 
	 * @param context			Context of the caller
	 * @param bitmap			Bitmap to be blurred
	 * @param blurRadius		Blur radius to apply, range between 1f and 25f
	 */
	public static Bitmap blurImage(Context context, Bitmap bitmap, float blurRadius, float sampleSize) {
		return applyBlur(context, bitmap, blurRadius, sampleSize, -1, -1, null, 0f);
	}
	
	
	/**
	 * Blurs a bitmap
	 * 
	 * @param context		Context of the caller
	 * @param bitmap		Bitmap to be blurred
	 * @param blurRadius	Blur radius to apply, range between 1f and 25f
	 * @param sampleSize	Scaling down the bitmap improves blurriness and performance significantly (value must >= 1)
	 * @param overlayColor	Color the image will be overlaid with (transparency should be included)
	 */
	public static Bitmap blurImage(Context context, Bitmap bitmap, float blurRadius, float sampleSize, int overlayColor) {
		return applyBlur(context, bitmap, blurRadius, sampleSize, overlayColor, -1, null, 0f);
	}
	
	
	/**
	 * Blurs a bitmap
	 * 
	 * @param context			Context of the caller
	 * @param bitmap			Bitmap to be blurred
	 * @param blurRadius		Blur radius to apply, range between 1f and 25f
	 * @param sampleSize		Scaling down the bitmap improves blurriness and performance significantly (value must >= 1)
	 * @param overlayResource	Resource the image will be overlaid with
	 * @param overlayOpacity	Opacity of the overlaying resource (between 0f for transparent and 1f for opaque)
	 */
	public static Bitmap blurImage(Context context, Bitmap bitmap, float blurRadius, float sampleSize, int overlayResource, float overlayOpacity) {
		return applyBlur(context, bitmap, blurRadius, sampleSize, -1, overlayResource, null, overlayOpacity);
	}
	
	
	/**
	 * Blurs a bitmap
	 * 
	 * @param context			Context of the caller
	 * @param bitmap			Bitmap to be blurred
	 * @param blurRadius		Blur radius to apply, range between 1f and 25f
	 * @param sampleSize		Scaling down the bitmap improves blurriness and performance significantly (value must >= 1)
	 * @param overlayDrawable	Drawable the image will be overlaid with
	 * @param overlayOpacity	Opacity of the overlaying resource (between 0f for transparent and 1f for opaque)
	 */
	public static Bitmap blurImage(Context context, Bitmap bitmap, float blurRadius, float sampleSize, Drawable overlayDrawable, float overlayOpacity) {
		return applyBlur(context, bitmap, blurRadius, sampleSize, -1, -1, overlayDrawable, overlayOpacity);
	}
	
	
	/*
	 * Apply blurring and return resulting bitmap
	 */
	private static Bitmap applyBlur(Context context, Bitmap bitmap, float blurRadius, float sampleSize, int overlayColor, int overlayResource, Drawable overlayDrawable, float overlayOpacity) {
		
		// Define a resulting Bitmap
		Bitmap resultBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() / sampleSize), (int) (bitmap.getHeight() / sampleSize), true);
		
		
		// Convert it to a mutable Bitmap
		resultBitmap = convertToMutable(resultBitmap, false);
		

		// Do this only if we want to apply a RS-blur
		if (blurRadius > 0f) {
			
			// Get a new RenderScript object
	    	RenderScript renderScript = RenderScript.create(context);
	    	
	        final Allocation input = Allocation.createFromBitmap(renderScript, resultBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SHARED);
	        final Allocation output = Allocation.createFromBitmap(renderScript, resultBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SHARED);
	        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
	        
	        script.setRadius(blurRadius);
	        script.setInput(input);
	        script.forEach(output);
	        
	        renderScript.finish();
	        output.copyTo(resultBitmap);
	        
	    	renderScript.destroy();
	    	renderScript = null;
    	
		}
		
		
		// Scale the resultBitmap back to its initial size
		resultBitmap = Bitmap.createScaledBitmap(resultBitmap, bitmap.getWidth(), bitmap.getHeight(), true);
		
		
		// We provided a overlayColor, add it to our bitmap
		if (overlayColor != -1) {			
			Canvas canvas = new Canvas(resultBitmap);
			
			Paint overlayPaint = new Paint();
			overlayPaint.setColor(overlayColor);
			
			canvas.drawRect(0, 0, resultBitmap.getWidth(), resultBitmap.getHeight(), overlayPaint);
		}
		
		
		// Return our result
		return resultBitmap;
		
	}
	
	
	/**
	 * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
	 * more memory that there is already allocated.
	 * 
	 * Code obtained from: 
	 * http://goo.gl/0MzckH
	 * 
	 * @param sourceBitmap		Source bitmap that will be converted to a mutable
	 * @param recycleBitmap		Set to true if the source bitmap should be recycled to free up memory (a copy will be created if this is set to false)
	 * @return a copy of sourceBitmap, which is mutable.
	 */
	public static Bitmap convertToMutable(Bitmap sourceBitmap, boolean recycleBitmap) {
		
		try {
	    	
	        // Dump the bytes that make up our bitmap into a temporary file
	        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

	        // Open a RandomAccessFile
	        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

	        // Get the width and height of the source bitmap.
	        int width = sourceBitmap.getWidth();
	        int height = sourceBitmap.getHeight();
	        Config type = sourceBitmap.getConfig();

	        // Copy the bytes into the file
	        FileChannel channel = randomAccessFile.getChannel();
	        MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, sourceBitmap.getRowBytes()*height);
	        sourceBitmap.copyPixelsToBuffer(map);
	        
	        // Recycle the source bitmap if we wish to do so
	        if (recycleBitmap) {
	        	sourceBitmap.recycle();
	        	System.gc();
	        }

	        // Create a new bitmap to load the bitmap again.
	        sourceBitmap = Bitmap.createBitmap(width, height, type);
	        
	        // Load the bytes back from our temporary file
	        map.position(0);
	        sourceBitmap.copyPixelsFromBuffer(map);
	        
	        // Close the temporary file and channel
	        channel.close();
	        randomAccessFile.close();

	        // Delete the temporary file
	        file.delete();

		} 
		
		catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } 
		
		catch (IOException e) {
	        e.printStackTrace();
	    } 

	    return sourceBitmap;
	    
	}
	
}
