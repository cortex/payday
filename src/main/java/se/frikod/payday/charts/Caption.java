package se.frikod.payday.charts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
    private Paint captionOverlayBorderStyle;

    private ScriptIntrinsicBlur theIntrinsic;
    private final RenderScript rs;

    private Rect rect;

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
        captionBackgroundStyle.setColor(Color.HSVToColor(new float[]{220f, 0.1f, .90f}));
        captionBackgroundStyle.setAlpha(100);
        captionBackgroundStyle.setStrokeWidth(1f);

        captionOverlayStyle = new Paint();
        captionOverlayStyle.setColor(Color.WHITE);
        captionOverlayStyle.setStyle(Paint.Style.FILL);
        captionOverlayStyle.setAlpha(200);

        captionOverlayBorderStyle = new Paint();
        captionOverlayBorderStyle.setColor(Color.LTGRAY);
        captionOverlayBorderStyle.setAlpha(50);
        captionOverlayBorderStyle.setStrokeWidth(5f);
        captionOverlayBorderStyle.setStyle(Paint.Style.STROKE);

        rs = RenderScript.create(view.getContext());
        theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        theIntrinsic.setRadius(5f);

    }

    public void resize(Rect rect){
        this.rect = rect;
        captionTextStyle.setTextSize(rect.width()/24);
        captionAmountStyle.setTextSize(rect.width()/24);
    }

    public void draw(Canvas canvas, Bar selectedBar){

        int captionRow = 1;
        float margin = 16;
        String descriptionFormat = "%s";
        String amountFormat = " %10.2f";
        float captionFontSize = captionTextStyle.getTextSize();

        canvas.drawRoundRect(new RectF(
                rect.left, rect.top,
                rect.right, rect.bottom),
                10f, 10f, captionOverlayStyle);

        canvas.drawRoundRect(new RectF(
                rect.left + 2, rect.top + 2,
                rect.right - 2, rect.bottom - 2 ),
                10f, 10f, captionOverlayBorderStyle);

        canvas.drawText(String.format("- %tF -", selectedBar.date.toDate()),
                rect.left + rect.width() / 2.0f - 100, captionRow * 2 * captionFontSize + rect.top, captionTextStyle);

        captionRow += 2;
        for (Transaction t : selectedBar.dayTransactions) {
            canvas.drawText(String.format(descriptionFormat, t.description),
                    rect.left + margin, captionRow * 1.5f * captionFontSize + rect.top,
                    captionTextStyle);
            String amount = String.format(amountFormat, t.amount);
            canvas.drawText(amount,
                    rect.right - margin - captionAmountStyle.measureText(amount),
                    captionRow * 1.5f * captionFontSize + rect.top,
                    captionAmountStyle);
            captionRow++;
        }

    }

}
