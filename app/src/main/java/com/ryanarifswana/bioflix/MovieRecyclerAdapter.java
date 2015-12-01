package com.ryanarifswana.bioflix;

import android.graphics.drawable.shapes.Shape;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ryanarifswana.bioflix.database.model.Session;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by ariftopcu on 11/27/15.
 */
public class MovieRecyclerAdapter extends RecyclerView.Adapter<MovieRecyclerAdapter.ViewHolder> {
    private List<Session> sessions;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View listItem;
        public ViewHolder(View v) {
            super(v);
            listItem = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MovieRecyclerAdapter(List<Session> sessionSet) {
        sessions = sessionSet;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MovieRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_movie_list_view, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        TextView movieNameView = (TextView) holder.listItem.findViewById(R.id.movieName);
        TextView viewerName = (TextView) holder.listItem.findViewById(R.id.viewerName);
        TextView recordDate = (TextView) holder.listItem.findViewById(R.id.recordDate);

        movieNameView.setText(sessions.get(position).getMovieName());
        viewerName.setText(sessions.get(position).getViewerName());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, ''yy", Locale.US);
        recordDate.setText(sdf.format(new Date(sessions.get(position).getStartTime())));
        if(position == sessions.size() - 1) {

        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return sessions.size();
    }
}
