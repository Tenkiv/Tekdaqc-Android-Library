package com.tenkiv.tekdaqc.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import com.tenkiv.tekdaqc.application.TekCast;
import com.tenkiv.tekdaqc.application.ATekdaqcActivity;
import com.tenkiv.tekdaqc.data.TekdaqcSession;

/**
 * Created by ideal on 6/5/14.
 */
public abstract class ATekdaqcFragment extends Fragment {

    /**
     * Tag used to add this fragment to the fragment manager
     */
    public static String FRAGMENT_TITLE;

    protected TekdaqcSession mSession;
    protected LocalBroadcastManager mLocalBroadcastMgr;
    private SessionUpdatedReceiver mSessionUpdatedReceiver;

    protected abstract void updateView();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setHomeButtonEnabled(true);
        mLocalBroadcastMgr.registerReceiver(mSessionUpdatedReceiver, new IntentFilter(TekCast.ACTION_SESSION_UPDATED));
    }

    @Override
    public void onPause() {
        super.onPause();
        mLocalBroadcastMgr.unregisterReceiver(mSessionUpdatedReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSessionUpdatedReceiver = new SessionUpdatedReceiver(this);
        mLocalBroadcastMgr = LocalBroadcastManager.getInstance(getActivity());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "." + FRAGMENT_TITLE;
    }

    /**
     * Convenience method that calls the attached activity's find view by id call
     *
     * @param res - resource id
     * @return {@link android.view.View}
     * @see {@link android.view.View#findViewById(int)}
     */
    public View findViewById(int res) {
        return getActivity().findViewById(res);
    }

    /**
     * Resource id for the animator that should be used when entering the fragment or returning from the back stack.
     *
     * @return
     */
    public int getAnimatorIn() {
        return android.R.animator.fade_in;
    }

    /**
     * Resource id for the animator that should be used when exiting or popping the fragment off the stack.
     *
     * @return
     */
    public int getAnimatorOut() {
        return android.R.animator.fade_out;
    }

    /**
     * Convenience method to cast the activity to {@link com.tenkiv.tekdaqc.application.ATekdaqcActivity}
     *
     * @return {@link com.tenkiv.tekdaqc.application.ATekdaqcActivity} instance
     * @throws ClassCastException
     */
    public ATekdaqcActivity getAbstractActivity() throws ClassCastException {
        Activity ret = getActivity();
        if (ret instanceof ATekdaqcActivity)
            return (ATekdaqcActivity) ret;

        throw new ClassCastException("This activity is not an instance of IdealActivity");
    }

    private static final class SessionUpdatedReceiver extends BroadcastReceiver {

        private final ATekdaqcFragment mFragment;

        SessionUpdatedReceiver(ATekdaqcFragment frag) {
            mFragment = frag;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String uuid = intent.getStringExtra(TekCast.EXTRA_SESSION_UUID);
            if (uuid != null && uuid.equalsIgnoreCase(mFragment.mSession.uuid)) {
                mFragment.updateView();
            }
        }
    }
}
