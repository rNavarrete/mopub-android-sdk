// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.Nullable;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.AdReport;

public class AdAlertGestureListener extends GestureDetector.SimpleOnGestureListener{
    private static final int MINIMUM_NUMBER_OF_ZIGZAGS_TO_FLAG = 4;
    private static final float MAXIMUM_THRESHOLD_X_IN_DIPS = 100;
    private static final float MAXIMUM_THRESHOLD_Y_IN_DIPS = 100;
    @Nullable private final AdReport mAdReport;

    private float mCurrentThresholdInDips = MAXIMUM_THRESHOLD_X_IN_DIPS;
    private float mPreviousPositionX;
    private boolean mHasCrossedLeftThreshold;
    private boolean mHasCrossedRightThreshold;
    private AdAlertReporter mAdAlertReporter;

    enum ZigZagState { UNSET, GOING_RIGHT, GOING_LEFT, FINISHED, FAILED}
    private int mNumberOfZigZags;
    private float mPivotPositionX;
    private ZigZagState mCurrentZigZagState = ZigZagState.UNSET;

    private View mView;

    boolean mIsClicked;

    AdAlertGestureListener(View view, @Nullable AdReport adReport) {
        super();
        if (view != null && view.getWidth() > 0) {
            mCurrentThresholdInDips = Math.min(MAXIMUM_THRESHOLD_X_IN_DIPS, view.getWidth() / 3f);
        }
        mIsClicked = false;
        mView = view;
        mAdReport = adReport;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mCurrentZigZagState == ZigZagState.FINISHED) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        // e1 is always the initial touch down event.
        // e2 is the true motion event
        if (isTouchOutOfBoundsOnYAxis(e1, e2)) {
            mCurrentZigZagState = ZigZagState.FAILED;
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        switch(mCurrentZigZagState) {
            case UNSET:
                mPivotPositionX = e1.getX();
                updateInitialState(e2.getX());
                break;
            case GOING_RIGHT:
                updateZig(e2.getX());
                break;
            case GOING_LEFT:
                updateZag(e2.getX());
                break;
            case FAILED:
                break;
            default:
                break;
        }

        mPreviousPositionX = e2.getX();

        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    void finishGestureDetection() {
        final Context context = mView.getContext();
        if (mCurrentZigZagState == ZigZagState.FINISHED && context != null) {
            mAdAlertReporter = new AdAlertReporter(context, mView, mAdReport);
            mAdAlertReporter.send();
        }
        reset();
    }

    void reset() {
        mNumberOfZigZags = 0;
        mCurrentZigZagState = ZigZagState.UNSET;
    }

    private boolean isTouchOutOfBoundsOnYAxis(MotionEvent e1, MotionEvent e2) {
        if (e1 == null || e2 == null) {
            return false;
        }
        final float initialY = e1.getY();
        final float currentY = e2.getY();
        return (Math.abs(currentY - initialY) > MAXIMUM_THRESHOLD_Y_IN_DIPS);
    }

    private void updateInitialState(float currentPositionX) {
        if (currentPositionX > mPivotPositionX) {
            mCurrentZigZagState = ZigZagState.GOING_RIGHT;
        }
    }

    private void updateZig(float currentPositionX) {
        if (rightThresholdReached(currentPositionX) && isMovingLeft(currentPositionX)) {
            mCurrentZigZagState = ZigZagState.GOING_LEFT;
            mPivotPositionX = currentPositionX;
        }
    }

    private void updateZag(float currentPositionX) {
        if (leftThresholdReached(currentPositionX) && isMovingRight(currentPositionX)) {
            mCurrentZigZagState = ZigZagState.GOING_RIGHT;
            mPivotPositionX = currentPositionX;
        }
    }

    private void incrementNumberOfZigZags() {
        mNumberOfZigZags++;
        if(mNumberOfZigZags >= MINIMUM_NUMBER_OF_ZIGZAGS_TO_FLAG) {
            mCurrentZigZagState = ZigZagState.FINISHED;
        }
    }

    private boolean rightThresholdReached(float currentPosition) {
        if (mHasCrossedRightThreshold) {
            return true;
        } else if (currentPosition >= mPivotPositionX + mCurrentThresholdInDips) {
            mHasCrossedLeftThreshold = false;
            mHasCrossedRightThreshold = true;
            return true;
        } else {
            return false;
        }
    }

    private boolean leftThresholdReached(float currentPosition) {
        if (mHasCrossedLeftThreshold) {
            return true;
        } else if (currentPosition <= mPivotPositionX - mCurrentThresholdInDips) {
            mHasCrossedRightThreshold = false;
            mHasCrossedLeftThreshold = true;
            incrementNumberOfZigZags();
            return true;
        } else {
            return false;
        }
    }

    private boolean isMovingRight(float currentPositionX) {
        return (currentPositionX > mPreviousPositionX);
    }

    private boolean isMovingLeft(float currentPositionX) {
        return (currentPositionX < mPreviousPositionX);
    }

    @Deprecated // for testing
    int getNumberOfZigzags() {
        return mNumberOfZigZags;
    }

    @Deprecated // for testing
    float getMinimumDipsInZigZag() {
        return mCurrentThresholdInDips;
    }

    @Deprecated // for testing
    ZigZagState getCurrentZigZagState() {
        return mCurrentZigZagState;
    }

    @Deprecated // for testing
    AdAlertReporter getAdAlertReporter(){
        return mAdAlertReporter;
    }

    void onResetUserClick() {
        mIsClicked = false;
    }

    boolean isClicked() {
        return mIsClicked;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mIsClicked = true;
        return super.onSingleTapUp(e);
    }
}
