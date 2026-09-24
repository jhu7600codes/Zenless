package com.zenless.game;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/** Shared adapter for shop, upgrade and meta rows. Subclasses only fill in the texts. */
public abstract class RowAdapter extends BaseAdapter {

    public static final class Row {
        public final TextView title, subtitle, cost, count;

        Row(View v) {
            title = (TextView) v.findViewById(R.id.title);
            subtitle = (TextView) v.findViewById(R.id.subtitle);
            cost = (TextView) v.findViewById(R.id.cost);
            count = (TextView) v.findViewById(R.id.count);
        }
    }

    private final int size;

    protected RowAdapter(int size) {
        this.size = size;
    }

    /** @return whether the row is affordable / enabled (dims it otherwise) */
    protected abstract boolean bind(int position, Row row);

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convert, ViewGroup parent) {
        if (convert == null) {
            convert = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_item, parent, false);
            convert.setTag(new Row(convert));
        }
        boolean ok = bind(position, (Row) convert.getTag());
        convert.setAlpha(ok ? 1f : 0.45f);
        return convert;
    }

    /** Rebind only the visible rows in place, cheaper than notifyDataSetChanged every second. */
    public static void refreshVisible(ListView list) {
        if (list.getVisibility() != View.VISIBLE || list.getAdapter() == null) return;
        int first = list.getFirstVisiblePosition();
        int headers = list.getHeaderViewsCount();
        for (int i = 0; i < list.getChildCount(); i++) {
            int pos = first + i - headers;
            View child = list.getChildAt(i);
            if (pos < 0 || !(child.getTag() instanceof Row)) continue;
            RowAdapter a = (RowAdapter) (list.getAdapter() instanceof android.widget.HeaderViewListAdapter
                    ? ((android.widget.HeaderViewListAdapter) list.getAdapter()).getWrappedAdapter()
                    : list.getAdapter());
            if (pos < a.getCount()) a.getView(pos, child, list);
        }
    }
}
