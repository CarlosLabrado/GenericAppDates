package com.app_labs.genericappdates;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.app_labs.genericappdates.fragments.CalendarFragment;
import com.app_labs.genericappdates.utilities.AndroidBus;
import com.facebook.login.LoginManager;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.squareup.otto.Bus;

import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Firebase mRef;

    private final String TAG = MainActivity.class.getSimpleName();

    /* Data from the authenticated user */
    private AuthData mAuthData;

    public static Bus bus;

    // Navigation Drawer
    private String[] navMenuTitles;

    private Stack<Integer> mDrawerStack;
    boolean isFirstRun = true;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.nav_view)
    NavigationView mNavigationView;

    @Override
    protected void onStart() {
        super.onStart();

        mRef = new Firebase("https://blazing-inferno-2048.firebaseio.com/");
        mAuthData = mRef.getAuth();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        bus = new AndroidBus();
        bus.register(this);

        mDrawerStack = new Stack<>();

        /**toolBar **/
        setUpToolBar();

        setUpDrawer();

        Firebase.setAndroidContext(this);

        if (savedInstanceState == null) {
            /**
             * This dummy fragment is to prevent the transition error when popping the fragment backStack
             * Error: Attempt to invoke virtual method 'boolean android.support.v4.app.Fragment.getAllowReturnTransitionOverlap()' on a null object reference
             * https://code.google.com/p/android/issues/detail?id=82832
             */
            FragmentManager fragmentManager = getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .add(R.id.container, new Fragment())
                    .addToBackStack("dummy")
                    .commit();
            // on first time display view for first nav item
            reactToDrawerClick(0);
        }

    }

    /**
     * Finishes to draw the drawer
     */
    private void setUpDrawer() {
        // load slide menu items
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(this);
        setCheckedDrawerItem(0);
    }

    private void setCheckedDrawerItem(int item) {
        mNavigationView.getMenu().getItem(item).setChecked(true);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_places) {
            reactToDrawerClick(0);
        } else if (id == R.id.nav_balance) {
            reactToDrawerClick(1);
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Slide menu item click listener
     */
    private class SlideMenuClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // display view for selected nav drawer item
            reactToDrawerClick(position);
        }
    }

    /**
     * Displaying fragment view for selected nav drawer list item or sending an action to the fragment
     */
    private void reactToDrawerClick(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;

        mDrawerStack.push(position);
        FragmentManager fragmentManager = getSupportFragmentManager();
        boolean addToBackStack = true;

        boolean isFragmentTransition = false;
        switch (position) {
            case 0:
                /**
                 * All this block prevents the first (Home) fragment to be recreated every time
                 * you click on it, we have to do this, because of the inner nested fragments
                 */
                Fragment lastFragment;
                if (!isFirstRun) {
                    int backStackCount = fragmentManager.getBackStackEntryCount();
                    lastFragment = fragmentManager.getFragments().get(backStackCount - 1);
                    if (lastFragment instanceof CalendarFragment) {
                        addToBackStack = false;
                    } else {
                        fragment = fragmentManager.getFragments().get(1);
                    }
                } else {
                    fragment = new CalendarFragment();
                    isFirstRun = false;
                }
                break;
            case 1:
//                fragment = new FirstFragment();
                break;
            default:
                break;
        }

        if (fragment != null && addToBackStack) { // addToBackStack will only be false if is the Home fragment
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.explode));
                fragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
            }
            String backStateName = fragment.getClass().getName();

            fragmentManager.beginTransaction()
                    .addToBackStack(backStateName)
                    .replace(R.id.container, fragment)
                    .commit();
            Log.d(TAG, "fragment added " + fragment.getTag());

            setTitle(navMenuTitles[position]);
            // update selected item and title, then close the drawer
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (!isFragmentTransition) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            Log.i(TAG, "Action");

        } else {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            // error in creating fragment
            Log.e(TAG, "Error in creating fragment");
        }
    }

    /**
     * sets up the top bar
     */
    private void setUpToolBar() {
        setSupportActionBar(toolbar);
        setActionBarTitle(getString(R.string.app_toolbar_title_main), getString(R.string.app_toolbar_sub_title_main), false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /**
     * Gets called from the fragments onResume and its because only the first doesn't have the up
     * button on the actionBar
     *
     * @param title          The title to show on the ActionBar
     * @param subtitle       The subtitle to show on the ActionBar
     * @param showNavigateUp if true, shows the up button
     */
    public void setActionBarTitle(String title, String subtitle, boolean showNavigateUp) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            if (subtitle != null) {
                getSupportActionBar().setSubtitle(subtitle);
            } else {
                getSupportActionBar().setSubtitle(null);
            }
            if (showNavigateUp) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }


    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        if (this.mRef != null) {
            /* logout of Firebase */
            mRef.unauth();
            /* Logout of any of the Frameworks. This step is optional, but ensures the user is not logged into
             * Facebook/Google+ after logging out of Firebase. */
            if (this.mAuthData.getProvider().equals("facebook")) {
                /* Logout from Facebook */
                LoginManager.getInstance().logOut();
            }
            finish();
        }
    }


    /**
     * We want to exit the app on many back pressed
     */
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
        int fragments = getSupportFragmentManager().getBackStackEntryCount();
        if (fragments > 2) {
            mDrawerStack.pop();
            setCheckedDrawerItem(mDrawerStack.peek());
            super.onBackPressed();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.exit_dialog_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.exit_dialog_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            // this will call for a finish on the top login activity
                            //LoginActivityBack.loginBus.post(true);
                        }
                    })
                    .setNegativeButton(R.string.exit_dialog_no, null)
                    .show();
        }
    }
}
