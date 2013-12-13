package se.frikod.payday;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class TypefaceTextView extends android.widget.TextView {

    private static Map<String, Typeface> mTypefaceCache;

    public TypefaceTextView(Context context) {
        this(context, null);
    }

    public TypefaceTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TypefaceTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        if (this.isInEditMode()) return;

        if (mTypefaceCache == null) {
            mTypefaceCache = new HashMap<String, Typeface>();
        }

        final TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.TypefaceStyle);

        if (styledAttributes != null) {
            final String typefaceAssetPath = styledAttributes.getString(R.styleable.TypefaceStyle_typeface);
            if (typefaceAssetPath != null) {
                Typeface typeface;

                if (mTypefaceCache.containsKey(typefaceAssetPath)) {
                    typeface = mTypefaceCache.get(typefaceAssetPath);
                } else {
                    AssetManager assets = context.getAssets();
                    typeface = Typeface.createFromAsset(assets, typefaceAssetPath);
                    mTypefaceCache.put(typefaceAssetPath, typeface);
                }
                setTypeface(typeface);
            }
            styledAttributes.recycle();
        }
    }
}

