package com.iflytek.wheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.iflytek.mscv5plusdemo.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author cncoderx
 */
public class WheelView extends View {
    boolean mCyclic;
    int mItemCount;
    int mItemWidth;
    int mItemHeight;
    Rect mClipRectTop;
    Rect mClipRectMiddle;
    Rect mClipRectBottom;

    TextPaint mTextPaint;
    TextPaint mSelectedTextPaint;

    WheelScroller mScroller;
    private boolean editable = true;

    final List<CharSequence> mEntries = new ArrayList<>();

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
        boolean cyclic = a.getBoolean(R.styleable.WheelView_wheelCyclic, false);
        int itemCount = a.getInt(R.styleable.WheelView_wheelItemCount, 9);
        int textSize = a.getDimensionPixelSize(R.styleable.WheelView_wheelTextSize, $sp(R.dimen.wheel_text_size));
        int textColor = a.getColor(R.styleable.WheelView_wheelTextColor, $color(R.color.wheel_text_color));
        int selectedTextColor = a.getColor(R.styleable.WheelView_wheelSelectedTextColor, $color(R.color.wheel_selected_text_color));
        CharSequence[] entries = a.getTextArray(R.styleable.WheelView_wheelEntries);
        a.recycle();

        mCyclic = cyclic;
        mItemCount = itemCount;

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);

        mSelectedTextPaint = new TextPaint();
        mSelectedTextPaint.setAntiAlias(true);
        mSelectedTextPaint.setTextAlign(Paint.Align.CENTER);
        mSelectedTextPaint.setTextSize(textSize);
        mSelectedTextPaint.setColor(selectedTextColor);

        if (entries != null && entries.length > 0) {
            mEntries.addAll(Arrays.asList(entries));
        }

        mScroller = new WheelScroller(context, this);
    }

    public boolean isEditable() {
        return editable;
    }

    public WheelView setEditable(boolean editable) {
        this.editable = editable;
        invalidate();
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == MeasureSpec.EXACTLY
                && heightSpecMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSpecSize, heightSpecSize);
        } else if (widthSpecMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSpecSize, getPrefHeight());
        } else if (heightSpecMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(getPrefWidth(), heightSpecSize);
        } else {
            setMeasuredDimension(getPrefWidth(), getPrefHeight());
        }
        updateClipRect();
    }

    private void updateClipRect() {
        int clipLeft = getPaddingLeft();
        int clipRight = getMeasuredWidth() - getPaddingRight();
        int clipTop = getPaddingTop();
        int clipBottom = getMeasuredHeight() - getPaddingBottom();
        int clipVMiddle = (clipTop + clipBottom) / 2;

        mClipRectMiddle = new Rect();
        mClipRectMiddle.left = clipLeft;
        mClipRectMiddle.right = clipRight;
        mClipRectMiddle.top = clipVMiddle - mItemHeight / 2;
        mClipRectMiddle.bottom = clipVMiddle + mItemHeight / 2;

        mClipRectTop = new Rect();
        mClipRectTop.left = clipLeft;
        mClipRectTop.right = clipRight;
        mClipRectTop.top = clipTop;
        mClipRectTop.bottom = clipVMiddle - mItemHeight / 2;

        mClipRectBottom = new Rect();
        mClipRectBottom.left = clipLeft;
        mClipRectBottom.right = clipRight;
        mClipRectBottom.top = clipVMiddle + mItemHeight / 2;
        mClipRectBottom.bottom = clipBottom;
    }

    int $dp(int resId) {
        return getResources().getDimensionPixelOffset(resId);
    }

    int $sp(int resId) {
        return getResources().getDimensionPixelSize(resId);
    }

    int $color(int resId) {
        return getResources().getColor(resId);
    }

    /**
     * @return 控件的预算宽度
     */
    public int getPrefWidth() {
        float textWidth = 0;
        for (int i = 0; i < mEntries.size(); i++) {
            float mt = mSelectedTextPaint.measureText(mEntries.get(i).toString());
            textWidth = Math.max(mt, textWidth);
        }
        mItemWidth = (int) textWidth;
        int paddingHorizontal = getPaddingLeft() + getPaddingRight();
        return paddingHorizontal + mItemWidth;
    }

    /**
     * @return 控件的预算高度
     */
    public int getPrefHeight() {
        Paint.FontMetricsInt fm = mSelectedTextPaint.getFontMetricsInt();
        mItemHeight = ~fm.top - (~fm.top - ~fm.ascent) - (fm.bottom - fm.descent);
        mItemHeight *= 1.2f;
        int paddingVertical = getPaddingTop() + getPaddingBottom();
        return paddingVertical + mItemHeight * mItemCount;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //  drawHighlight(canvas);
        drawItems(canvas);
        // drawDivider(canvas);
    }

    private void drawItems(Canvas canvas) {
        final int index = mScroller.getItemIndex();
        final int offset = mScroller.getItemOffset();

        //Log.v("view", "index:" + index + " offset:" + offset);
        final int hf = (mItemCount + 1) / 2;
        final int minIdx, maxIdx;
        if (offset < 0) {
            minIdx = index - hf - 1;
            maxIdx = index + hf;
        } else if (offset > 0) {
            minIdx = index - hf;
            maxIdx = index + hf + 1;
        } else {
            minIdx = index - hf;
            maxIdx = index + hf;
        }
        for (int i = minIdx; i < maxIdx; i++) {
            drawItem2(canvas, i, offset);
        }


//        for (int i = 0; i < mEntries.size(); i++) {
//            drawItem2(canvas, i, offset);
//        }
    }

    private void drawItem2(Canvas canvas, int index, int offset) {
        if (!editable && index != mScroller.getCurrentIndex()) {
            return;
        }
        CharSequence text = getCharSequence(index);
        if (text == null) return;
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        int baseline = (int) ((fontMetrics.top + fontMetrics.bottom) / 2);
        // 和中间选项的距离
        final int drawY = mClipRectMiddle.centerY() + (index - mScroller.getItemIndex()) * mItemHeight - offset - baseline;
        float sv = (2 * mItemCount * mItemHeight - (float) Math.abs(mClipRectMiddle.centerY() - drawY - baseline)) / (2 * mItemCount * mItemHeight);
        canvas.save();
        canvas.scale(sv, sv, mClipRectMiddle.centerX(), drawY);
        canvas.drawText(text, 0, text.length(), mClipRectMiddle.centerX(), drawY, (index == mScroller.getCurrentIndex()) ? mSelectedTextPaint : mTextPaint);
        canvas.restore();
    }

    protected void drawItem(Canvas canvas, int index, int offset) {
        CharSequence text = getCharSequence(index);
        if (text == null) return;

        final int centerX = mClipRectMiddle.centerX();
        final int centerY = mClipRectMiddle.centerY();

        // 和中间选项的距离
        final int range = (index - mScroller.getItemIndex()) * mItemHeight - offset;

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        int baseline = (int) ((fontMetrics.top + fontMetrics.bottom) / 2);

        // 绘制与下分界线相交的文字
        if (range > 0 && range < mItemHeight) {
            canvas.save();
            canvas.clipRect(mClipRectMiddle);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mSelectedTextPaint);
            canvas.restore();

            canvas.save();
            canvas.clipRect(mClipRectBottom);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mTextPaint);
            canvas.restore();
        }
        // 绘制下分界线下方的文字
        else if (range >= mItemHeight) {
            canvas.save();
            canvas.clipRect(mClipRectBottom);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mTextPaint);
            canvas.restore();
        }
        // 绘制与上分界线相交的文字
        else if (range < 0 && range > -mItemHeight) {
            canvas.save();
            canvas.clipRect(mClipRectMiddle);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mSelectedTextPaint);
            canvas.restore();

            canvas.save();
            canvas.clipRect(mClipRectTop);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mTextPaint);
            canvas.restore();
        }
        // 绘制上分界线上方的文字
        else if (range <= -mItemHeight) {
            canvas.save();
            canvas.clipRect(mClipRectTop);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mTextPaint);
            canvas.restore();
        }
        // 绘制两条分界线之间的文字
        else {
            canvas.save();
            canvas.clipRect(mClipRectMiddle);
            canvas.drawText(text, 0, text.length(), centerX, centerY + range - baseline, mSelectedTextPaint);
            canvas.restore();
        }
    }

    public CharSequence getCharSequence(int index) {
        int size = mEntries.size();
        if (size == 0) return null;
        CharSequence text = null;
        if (isCyclic()) {
            int i = index % size;
            if (i < 0) {
                i += size;
            }
            text = mEntries.get(i);
        } else {
            if (index >= 0 && index < size) {
                text = mEntries.get(index);
            }
        }
        return text;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editable) {
            return super.onTouchEvent(event);
        }
        return mScroller.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        mScroller.computeScroll();
    }

    public boolean isCyclic() {
        return mCyclic;
    }

    public void setCyclic(boolean cyclic) {
        mCyclic = cyclic;
        mScroller.reset();
        invalidate();
    }

    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    public void setTextSize(int textSize) {
        mTextPaint.setTextSize(textSize);
        mSelectedTextPaint.setTextSize(textSize);
        invalidate();
    }

    public int getTextColor() {
        return mTextPaint.getColor();
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
        invalidate();
    }

    public int getSelectedTextColor() {
        return mSelectedTextPaint.getColor();
    }

    public void setSelectedTextColor(int color) {
        mSelectedTextPaint.setColor(color);
        invalidate();
    }

    public int getItemSize() {
        return mEntries.size();
    }

    public CharSequence getItem(int index) {
        if (index < 0 && index >= mEntries.size())
            return null;

        return mEntries.get(index);
    }

    public CharSequence getCurrentItem() {
        return getItem(getCurrentIndex());
    }

    public int getCurrentIndex() {
        return mScroller.getCurrentIndex();
    }

    public void setCurrentIndex(int index) {
        OnWheelChangedListener l = getOnWheelChangedListener();
        setOnWheelChangedListener(null);
        setCurrentIndex(index, false);
        setOnWheelChangedListener(l);
    }

    public void setCurrentIndex(int index, boolean animated) {
        mScroller.setCurrentIndex(index, animated);
    }

    public void setRange(int start, int end) {
        OnWheelChangedListener l = getOnWheelChangedListener();
        setOnWheelChangedListener(null);
        int ci = mScroller.getCurrentIndex();
        mEntries.clear();
        for (int i = start; i <= end; i++) {
            mEntries.add((i < 10 ? "0" : "") + i);
        }
        if (mItemWidth == 0) {
            mScroller.reset();
            requestLayout();
        } else {
            if (ci > mEntries.size() - 1) {
                mScroller.setCurrentIndex(mEntries.size() - 1, true);
            }
            invalidate();
        }
        setOnWheelChangedListener(l);
    }

    public void setEntries(CharSequence... entries) {
        mEntries.clear();
        if (entries != null && entries.length > 0) {
            Collections.addAll(mEntries, entries);
        }
        mScroller.reset();
        requestLayout();
    }

    public void setEntries(Collection<? extends CharSequence> entries) {
        mEntries.clear();
        if (entries != null && entries.size() > 0) {
            mEntries.addAll(entries);
        }
        mScroller.reset();
        invalidate();
    }

    public OnWheelChangedListener getOnWheelChangedListener() {
        return mScroller.onWheelChangedListener;
    }

    public void setOnWheelChangedListener(OnWheelChangedListener onWheelChangedListener) {
        mScroller.onWheelChangedListener = onWheelChangedListener;
    }

    public int getCurrentValue() {
        try {
            return Integer.parseInt(getCharSequence(mScroller.getCurrentIndex()).toString());
        } catch (Exception e) {
            return mScroller.getCurrentIndex() + 1;
        }
    }

    public String getCurrentStr() {
        int value = getCurrentValue();
        return (value < 10 ? "0" : "") + value;

    }

    public boolean scrolling() {
        return !mScroller.isFinished();
    }
}
