package de.dala.simplenews.ui;

import android.content.ActivityNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import circularmenu.FloatingActionButton;
import circularmenu.FloatingActionMenu;
import circularmenu.SubActionButton;
import de.dala.simplenews.R;
import de.dala.simplenews.common.Category;
import de.dala.simplenews.database.DatabaseHandler;
import de.dala.simplenews.utilities.PrefUtilities;
import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Created by Daniel on 20.02.14.
 */
public class NewsOverViewFragment extends Fragment implements ViewPager.OnPageChangeListener {

    private PagerSlidingTabStrip tabs;

    private View progressView;
    private Drawable oldBackground = null;
    private int currentColor = 0xFF666666;
    private List<Category> categories;
    private RelativeLayout bottomView;
    private Crouton crouton;
    private int loadingNews = -1;
    private MainActivity mainActivity;
    private int entryType = ALL;
    private FloatingActionMenu newsTypeButton;
    private MenuItem columnsMenu;

    public static final int ALL = 1;
    public static final int FAV = 2;
    public static final int RECENT = 3;
    public static final int UNREAD = 4;

    public FloatingActionMenu getNewsTypeButton() {
        return newsTypeButton;
    }

    public interface INewsTypeButton{
        void newsTypeModeChanged(int newsTypeMode);
        void gridSettingsChanged();
    }

    private ArrayList<String> newsTypeTags;

    public NewsOverViewFragment() {
    }

    public static Fragment getInstance(int entryType) {
        Fragment fragment = new NewsOverViewFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("entryType", entryType);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            this.mainActivity = (MainActivity) getActivity();
        } else {
            throw new ActivityNotFoundException("MainActivity not found");
        }
        entryType = getArguments().getInt("entryType", ALL);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.news_overview_menu, menu);
        columnsMenu = menu.findItem(R.id.menu_columns);
        updateMenu();
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void updateMenu(){
        boolean useMultiple = shouldUseMultipleColumns();
        if (columnsMenu != null){
            columnsMenu.setTitle(useMultiple ? getString(R.string.single_columns) : getString(R.string.multiple_columns));
        }
    }

    private boolean shouldUseMultipleColumns(){
        boolean useMultiple = false;
        switch (getResources().getConfiguration().orientation) {
            case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                useMultiple = PrefUtilities.getInstance().useMultipleColumnsLandscape();
                break;
            case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                useMultiple = PrefUtilities.getInstance().useMultipleColumnsPortrait();
                break;
        }
        return useMultiple;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_columns:
                switch (getResources().getConfiguration().orientation) {
                    case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                        PrefUtilities.getInstance().setMultipleColumnsLandscape(!PrefUtilities.getInstance().useMultipleColumnsLandscape());
                        break;
                    case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                        PrefUtilities.getInstance().setMultipleColumnsPortrait(!PrefUtilities.getInstance().useMultipleColumnsPortrait());
                        break;
                }
                updateMenu();
                updateColumnCount();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateColumnCount() {
        for (INewsTypeButton newsTypeButton: getActiveINewsTypeFragments()){
            newsTypeButton.gridSettingsChanged();
        }
    }

    private List<INewsTypeButton> getActiveINewsTypeFragments(){
        List<INewsTypeButton> fragments = new ArrayList<INewsTypeButton>();
        for (Iterator<String> iterator = newsTypeTags.iterator();
             iterator.hasNext(); )
        {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(iterator.next());
            if (fragment != null && fragment instanceof INewsTypeButton){
                fragments.add((INewsTypeButton)fragment);
            }else{
                iterator.remove();
            }
        }
        return fragments;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.news_overview, container, false);
        if (!PrefUtilities.getInstance().xmlIsAlreadyLoaded()) {
            DatabaseHandler.getInstance().loadXmlIntoDatabase(R.raw.categories);
        }
        mainActivity.getSupportActionBar().setTitle(getString(R.string.simple_news_title));
        mainActivity.getSupportActionBar().setHomeButtonEnabled(true);
        categories = DatabaseHandler.getInstance().getCategories(null, null, true);


        if (savedInstanceState != null){
            Serializable newsTypeTagsSerializable = savedInstanceState.getSerializable("newsTypeTags");
            if (newsTypeTagsSerializable != null && newsTypeTagsSerializable instanceof ArrayList<?>){
                newsTypeTags = (ArrayList<String>) newsTypeTagsSerializable;
            }
            entryType = savedInstanceState.getInt("entryType", ALL);
            newsTypeModeChanged();
        }else{
            newsTypeTags = new ArrayList<String>();
        }

        if (categories != null && !categories.isEmpty()) {
            ViewPager pager = (ViewPager) rootView.findViewById(R.id.pager);
            MyPagerAdapter adapter = new MyPagerAdapter(getChildFragmentManager());
            pager.setAdapter(adapter);

            tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabs);
            tabs.setViewPager(pager);
            tabs.setOnPageChangeListener(this);

            final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                    .getDisplayMetrics());
            pager.setPageMargin(pageMargin);
            bottomView = (RelativeLayout) rootView.findViewById(R.id.bottom_view);
            createProgressView();

            onPageSelected(0);
        }

        initNewsTypeIcon(null); //rootView.findViewById(R.id.news_layer)); //null
        return rootView;
    }

    private void changeColor(Category category) {

        tabs.setIndicatorColor(category.getSecondaryColor());

        Drawable colorDrawable = new ColorDrawable(category.getPrimaryColor());
        Drawable darkColorDrawable = new ColorDrawable(category.getSecondaryColor());

        Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);

        LayerDrawable ld = new LayerDrawable(new Drawable[]{colorDrawable, bottomDrawable});

        if (oldBackground == null) {
            ((ActionBarActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(ld);
        } else {
            //getSupportActionBar().setBackgroundDrawable(ld); //BUG otherwise
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, ld});
            ((ActionBarActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(td);
            td.startTransition(400);
        }

        mainActivity.changeDrawerColor(category.getPrimaryColor());

        progressView.setBackgroundColor(category.getPrimaryColor());
        oldBackground = ld;
        currentColor = category.getPrimaryColor();

        // http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    private SubActionButton subactionButton1;
    private SubActionButton subactionButton2;
    private SubActionButton subactionButton3;
    private ImageView mainIcon;
    private FloatingActionButton button;

    private void initNewsTypeIcon(View rootLayer){
        mainIcon = new ImageView(getActivity());
        button = new FloatingActionButton.Builder(getActivity())
                .setContentView(mainIcon)
                .setAttachingView(rootLayer)
                .build();

        int subButtonSize = getResources().getDimensionPixelSize(R.dimen.sub_action_button_size_medium);
        int actionMenuRadius = getResources().getDimensionPixelSize(R.dimen.action_menu_radius);
        FrameLayout.LayoutParams subButtonParams = new FrameLayout.LayoutParams(subButtonSize, subButtonSize);

        SubActionButton.Builder rLSubBuilder = new SubActionButton.Builder(getActivity()).setLayoutParams(subButtonParams);
        ImageView icon1 = new ImageView(getActivity());
        ImageView icon2 = new ImageView(getActivity());
        ImageView icon3 = new ImageView(getActivity());

        subactionButton1 = rLSubBuilder.setContentView(icon1).build();
        subactionButton2 = rLSubBuilder.setContentView(icon2).build();
        subactionButton3 = rLSubBuilder.setContentView(icon3).build();

        subactionButton1.setOnClickListener(new OnSubActionButtonClickListener());
        subactionButton2.setOnClickListener(new OnSubActionButtonClickListener());
        subactionButton3.setOnClickListener(new OnSubActionButtonClickListener());

        // Build the menu with default options: light theme, 90 degrees, 72dp radius.
        // Set 4 default SubActionButtons
        newsTypeButton =  new FloatingActionMenu.Builder(getActivity())
                .addSubActionView(subactionButton1)
                .addSubActionView(subactionButton2)
                .addSubActionView(subactionButton3)
                .setRadius(actionMenuRadius)
                .attachTo(button)
                .setAttachingView(rootLayer)
                .build();
        updateNewsIcon();
    }

    private void updateNewsIcon(){
        switch (entryType){
            case ALL:
                subactionButton1.setTag(UNREAD);
                subactionButton2.setTag(FAV);
                subactionButton3.setTag(RECENT);
                ((ImageView)subactionButton1.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_new));
                ((ImageView)subactionButton2.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_favorite));
                ((ImageView)subactionButton3.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_seen));
                mainIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_home));
                break;
            case UNREAD:
                subactionButton1.setTag(ALL);
                subactionButton2.setTag(FAV);
                subactionButton3.setTag(RECENT);
                ((ImageView)subactionButton1.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_home));
                ((ImageView)subactionButton2.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_favorite));
                ((ImageView)subactionButton3.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_seen));
                mainIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_new));
                break;
            case FAV:
                subactionButton1.setTag(UNREAD);
                subactionButton2.setTag(ALL);
                subactionButton3.setTag(RECENT);
                ((ImageView)subactionButton1.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_new));
                ((ImageView)subactionButton2.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_home));
                ((ImageView)subactionButton3.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_seen));
                mainIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_favorite));
                break;
            case RECENT:
                subactionButton1.setTag(UNREAD);
                subactionButton2.setTag(FAV);
                subactionButton3.setTag(ALL);
                ((ImageView)subactionButton1.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_new));
                ((ImageView)subactionButton2.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_favorite));
                ((ImageView)subactionButton3.getContentView()).setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_home));
                mainIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_nav_seen));
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        newsTypeButton.close(false);
        button.detach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("newsTypeTags", newsTypeTags);
        outState.putInt("entryType", entryType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int i) {
        if (categories != null && categories.size() > i) {
            changeColor(categories.get(i));
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void updateNews() {
        if (crouton != null) {
            crouton.cancel();
        }
        Configuration config = new Configuration.Builder().setOutAnimation(R.anim.abc_slide_out_bottom).setInAnimation(R.anim.abc_slide_in_bottom).setDuration(Configuration.DURATION_INFINITE).build();
        crouton = Crouton.make(getActivity(), createProgressView(), bottomView);
        crouton.setConfiguration(config);
        crouton.show();
    }

    public void showLoadingNews() {
        loadingNews++;
        if (loadingNews == 0) {
            updateNews();
        }
    }

    public void cancelLoadingNews() {
        loadingNews--;
        if (loadingNews >= -1) {
            crouton.cancel();
            loadingNews = -1;
        }
    }

    private View createProgressView() {
        progressView = mainActivity.getLayoutInflater().inflate(R.layout.progress_layout, null);
        progressView.setBackgroundColor(currentColor);
        TextView progressText = (TextView) progressView.findViewById(R.id.progress_text);
        progressText.setText(getString(R.string.update_news));
        return progressView;
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {

        private FragmentManager mFragmentManager;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return categories.get(position).getName();
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Fragment getItem(int position) {
            // Check if this Fragment already exists.
            String name = makeFragmentName(R.id.pager, position);
            if (!newsTypeTags.contains(name)){
                newsTypeTags.add(name);
            }

            Fragment fragment = mFragmentManager.findFragmentByTag(name);
            if(fragment == null){
                Category category = categories.get(position);
                fragment = ExpandableNewsFragment.newInstance(category, entryType);
            }
            return fragment;
        }

        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }
    }

    private class OnSubActionButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            entryType = (Integer) v.getTag();
            updateNewsIcon();
            newsTypeModeChanged();
            newsTypeButton.close(true);
        }
    }

    private void newsTypeModeChanged() {
        for (INewsTypeButton newsTypeButton: getActiveINewsTypeFragments()){
            newsTypeButton.newsTypeModeChanged(entryType);
        }
    }

}
