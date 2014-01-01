package se.frikod.payday.charts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import se.frikod.payday.Transaction;
import se.frikod.payday.TransactionsGraphView;

/**
 * Created by joakim on 12/28/13.
 */
class Caption{

    private Paint captionTextStyle;
    private Paint captionAmountStyle;
    private Paint captionBackgroundStyle;
    private Paint captionOverlayStyle;

    private ScriptIntrinsicBlur theIntrinsic;
    private final RenderScript rs;

    Caption(TransactionsGraphView view){
        float captionFontSize = view.getWidth() / 80f;
        captionTextStyle = new Paint();
        captionTextStyle.setColor(Color.DKGRAY);
        captionTextStyle.setTextSize(captionFontSize);
        Typeface textFont = Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Regular.ttf");

        captionTextStyle.setTypeface(textFont);
        captionTextStyle.setAntiAlias(true);

        captionAmountStyle = new Paint();
        captionAmountStyle.setColor(Color.DKGRAY);
        captionAmountStyle.setTextSize(captionFontSize);
        captionAmountStyle.setTypeface(Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Light.ttf"));
        captionAmountStyle.setAntiAlias(true);


        captionBackgroundStyle = new Paint();
        //captionBackgroundStyle.setColor(Color.HSVToColor(new float[]{220f, 0.1f, .90f}));
        //captionBackgroundStyle.setAlpha(100);
        //captionBackgroundStyle.setStrokeWidth(1f);

        captionOverlayStyle = new Paint();
        captionOverlayStyle.setColor(Color.WHITE);
        //captionOverlayStyle.setColor(0xFFF5F5FF);
        captionOverlayStyle.setColor(Color.HSVToColor(new float[]{230f, 0.01f, 1f}));

        captionOverlayStyle.setAlpha(190);

        rs = RenderScript.create(view.getContext());
        theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        theIntrinsic.setRadius(5f);

    }

    public void resize(int w, int h){
        captionTextStyle.setTextSize(w/24);
        captionAmountStyle.setTextSize(w/24);
    }

    public void draw(Canvas canvas, Bar selectedBar){

        int captionRow = 1;
        float margin = 20f;

        String descriptionFormat = "%s";
        String amountFormat = " %10.2f";
        float captionFontSize = captionTextStyle.getTextSize();

        //float textWidth = captionTextStyle.measureText(String.format(format, "test test test test", 10000f));
        //float textWidth = findMaxFontSize(canvas.getWidth() - 4 * margin , captionTextStyle, );

        float top, bottom;
        Path path = new Path();
        float boxHeight = captionTextStyle.getFontSpacing() * (4 +  selectedBar.dayTransactions.size());

        top = canvas.getHeight() / 2f;
        bottom = canvas.getHeight();

        canvas.drawRoundRect(new RectF(
                margin, top,
                (2 * margin) + canvas.getWidth() - 2*margin, bottom),
                10f, 10f, captionOverlayStyle);

        canvas.drawPath(path, captionBackgroundStyle);

        canvas.drawText(String.format("- %tF -", selectedBar.date.toDate()),
                canvas.getWidth() / 2.0f - 100, captionRow * 2 * captionFontSize + top, captionTextStyle);

        captionRow += 2;
        for (Transaction t : selectedBar.dayTransactions) {
            canvas.drawText(String.format(descriptionFormat, t.description),
                    2*margin, captionRow * 1.5f * captionFontSize + top,
                    captionTextStyle);
            String amount = String.format(amountFormat, t.amount);
            canvas.drawText(amount,
                    canvas.getWidth() - 2 * margin - captionAmountStyle.measureText(amount),
                    captionRow * 1.5f * captionFontSize + top,
                    captionAmountStyle);
            captionRow++;
        }

    }

}
