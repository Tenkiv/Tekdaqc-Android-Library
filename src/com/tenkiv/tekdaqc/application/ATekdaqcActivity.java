package com.tenkiv.tekdaqc.application;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.view.View;
import com.tenkiv.tekdaqc.fragments.ATekdaqcFragment;

/**
 * Created by ideal on 6/5/14.
 */
public abstract class ATekdaqcActivity extends Activity {

    /**
     * The resource id of the layout the fragment replace.
     *
     * @return
     */
    public int getFragmentContainerId() {
        return View.NO_ID;
    }

    /**
     * Replace the fragment container of the activity with the specified fragment. The added fragment will be placed on
     * the back stack.
     *
     * @param fragment
     * @param tag
     */
    public void replaceFragment(ATekdaqcFragment fragment, String tag) {
        replaceFragment(fragment, tag, true);
    }

    /**
     * Replace the fragment container of the activity with the specified fragment. Optionally place the fragment on the
     * back stack.
     *
     * @param fragment
     * @param tag
     * @param addToBackStack
     */
    public void replaceFragment(ATekdaqcFragment fragment, String tag, boolean addToBackStack) {
        final int containerID = getFragmentContainerId();
        if (containerID == View.NO_ID)
            throw new IllegalArgumentException("Calling activity must override getFragmentContainerId.");

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (addToBackStack)
            ft.addToBackStack(null);
        ft.setCustomAnimations(fragment.getAnimatorIn(), fragment.getAnimatorOut(), fragment.getAnimatorIn(),
                fragment.getAnimatorOut());
        ft.replace(containerID, fragment, tag);
        ft.commit();
    }
}
