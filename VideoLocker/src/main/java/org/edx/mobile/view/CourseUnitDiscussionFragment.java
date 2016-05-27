package org.edx.mobile.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.edx.mobile.R;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.model.course.DiscussionBlockModel;
import org.edx.mobile.services.ViewPagerDownloadManager;

public class CourseUnitDiscussionFragment extends CourseUnitFragment {
    public static final String FRAG_TAG = "discussion_frag";

    /**
     * Create a new instance of fragment
     */
    static CourseUnitDiscussionFragment newInstance(CourseComponent unit, EnrolledCoursesResponse courseData) {
        CourseUnitDiscussionFragment f = new CourseUnitDiscussionFragment();
        Bundle args = new Bundle();
        args.putSerializable(Router.EXTRA_COURSE_UNIT, unit);
        args.putSerializable(Router.EXTRA_ENROLLMENT_COURSE_DATA, courseData);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * The Fragment's UI is just a FrameLayout which nests the CourseDiscussionPostsThreadFragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_course_unit_discussion, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // First we need to get the discussion topic id to send to the posts fragment
        String topicId = ((DiscussionBlockModel)getArguments().getSerializable(
                Router.EXTRA_COURSE_UNIT)).getData().topicId;

        Bundle args = new Bundle();
        args.putString(Router.EXTRA_DISCUSSION_TOPIC_ID, topicId);
        args.putSerializable(Router.EXTRA_ENROLLMENT_COURSE_DATA,
                getArguments().getSerializable(Router.EXTRA_ENROLLMENT_COURSE_DATA));
        Fragment fragment = new CourseDiscussionPostsThreadFragment();
        fragment.setArguments(args);
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.content, fragment, FRAG_TAG);
        fragmentTransaction.disallowAddToBackStack();
        fragmentTransaction.commit();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (ViewPagerDownloadManager.instance.inInitialPhase(unit))
            ViewPagerDownloadManager.instance.addTask(this);
    }


    @Override
    public void run() {
        ViewPagerDownloadManager.instance.done(this, true);
    }

}
