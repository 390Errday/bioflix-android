package com.ryanarifswana.bioflix;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.ryanarifswana.bioflix.database.DatabaseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by ariftopcu on 11/26/15.
 */
public class MainActivity extends AppCompatActivity {

    DatabaseHandler db;

    private CoordinatorLayout coordinatorLayout;
    private MainActivity mActivity;

    private RecyclerView movieRecyclerView;
    private TextView emptyRecyclerText;
    private RecyclerView.Adapter recyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        db = new DatabaseHandler(this);
        setContentView(R.layout.activity_main);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setFloatingActionButton();
        setMovieRecyclerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.hr_consent) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startNewSessionActivity() {
        Intent intent = new Intent(mActivity, NewSessionActivity.class);
        startActivity(intent);
    }

    private void setFloatingActionButton() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startNewSessionActivity();
            }
        });
    }

    private void setMovieRecyclerView() {
        List<Session> allSessions = db.getAllSessions();
        movieRecyclerView = (RecyclerView) findViewById(R.id.main_movies_list);
        emptyRecyclerText = (TextView) findViewById(R.id.empty_recycler);
        if(allSessions.size() > 0) {
            movieRecyclerView.setVisibility(View.VISIBLE);
            emptyRecyclerText.setVisibility(View.INVISIBLE);

            movieRecyclerView.setHasFixedSize(true);
            movieRecyclerView.addItemDecoration(new DividerItemDecoration(this));

            // use a linear layout manager
            recyclerLayoutManager = new LinearLayoutManager(this);
            movieRecyclerView.setLayoutManager(recyclerLayoutManager);

            // specify an adapter (see also next example)
            recyclerAdapter = new MovieRecyclerAdapter(allSessions);
            movieRecyclerView.setAdapter(recyclerAdapter);
        }
        else {
            movieRecyclerView.setVisibility(View.INVISIBLE);
            emptyRecyclerText.setVisibility(View.VISIBLE);
        }

    }

    private class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private final int[] ATTRS = new int[]{android.R.attr.listDivider};

        private Drawable mDivider;

        /**
         * Default divider will be used
         */
        public DividerItemDecoration(Context context) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
            mDivider = styledAttributes.getDrawable(0);
            styledAttributes.recycle();
        }

        /**
         * Custom divider will be used
         */
        public DividerItemDecoration(Context context, int resId) {
            mDivider = ContextCompat.getDrawable(context, resId);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}


