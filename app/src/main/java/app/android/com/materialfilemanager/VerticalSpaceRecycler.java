package app.android.com.materialfilemanager;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Cosimo Sguanci on 12/09/2016.
 */
public class VerticalSpaceRecycler extends RecyclerView.ItemDecoration {
    private final int mVerticalSpaceHeight;

    public VerticalSpaceRecycler(int mVerticalSpaceHeight) {
        this.mVerticalSpaceHeight = mVerticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.bottom = mVerticalSpaceHeight;
    }
}

