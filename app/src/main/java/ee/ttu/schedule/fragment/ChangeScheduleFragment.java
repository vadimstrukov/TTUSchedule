package ee.ttu.schedule.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.vadimstrukov.ttuschedule.R;

import java.util.HashMap;
import java.util.Map;

import ee.ttu.schedule.provider.BaseContract;
import ee.ttu.schedule.provider.GroupContract;
import ee.ttu.schedule.utils.SyncUtils;

public class ChangeScheduleFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.MultiChoiceModeListener, AdapterView.OnItemClickListener, SearchView.OnQueryTextListener, SyncStatusObserver, SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = "ChangeScheduleFragment";

    private ListView groupListView;
    private TextView emptyText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CursorAdapter groupCursorAdapter;

    private Object syncObserverHandle;

    private Map<Long, String> groupMap;

    private SyncUtils syncUtils;

    private static final String GROUP_FRAGMENT = "group_fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(0, new Bundle(), this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        syncUtils = new SyncUtils(getActivity());
        groupCursorAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_multiple_choice, null,
                new String[]{GroupContract.GroupColumns.KEY_NAME}, new int[]{android.R.id.text1}, 0);
        groupListView.setAdapter(groupCursorAdapter);
        groupListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        groupListView.setMultiChoiceModeListener(this);
        groupListView.setOnItemClickListener(this);
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_schedule, container, false);
        groupListView = (ListView) view.findViewById(R.id.groupListView);
        emptyText = (TextView) view.findViewById(android.R.id.empty);
        groupListView.setEmptyView(emptyText);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        onStatusChanged(0);
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        syncObserverHandle = ContentResolver.addStatusChangeListener(mask, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(syncObserverHandle !=null){
            ContentResolver.removeStatusChangeListener(syncObserverHandle);
            syncObserverHandle = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_group, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = GroupContract.GroupColumns.KEY_NAME + " ASC";
        String selection = String.format("%1$s like ?", GroupContract.GroupColumns.KEY_NAME);
        String[] selectionArgs = new String[]{"%" + args.getString(GROUP_FRAGMENT, "") + "%"};
        return new CursorLoader(getActivity(), GroupContract.Group.CONTENT_URI, null, selection, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        groupCursorAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        groupCursorAdapter.changeCursor(null);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked)
            groupMap.put(id, groupCursorAdapter.getCursor().getString(1));
        else
            groupMap.remove(id);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        groupMap = new HashMap<>();
        mode.getMenuInflater().inflate(R.menu.menu_cab_group, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        swipeRefreshLayout.setRefreshing(true);
        for(Map.Entry<Long, String> entry : groupMap.entrySet()){
            syncUtils.syncEvents(entry.getValue());
        }
        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Bundle bundle = new Bundle();
        bundle.putString(GROUP_FRAGMENT, newText);
        getLoaderManager().restartLoader(0, bundle, this);
        return false;
    }

    @Override
    public void onStatusChanged(int which) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                NetworkInfo networkInfo = ((ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                if (networkInfo == null) {
                    ContentResolver.cancelSync(syncUtils.getAccount(), BaseContract.CONTENT_AUTHORITY);
                    Toast.makeText(getActivity(), getActivity().getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                }
                boolean syncActive = ContentResolver.isSyncActive(syncUtils.getAccount(), BaseContract.CONTENT_AUTHORITY);
                boolean syncPending = ContentResolver.isSyncPending(syncUtils.getAccount(), BaseContract.CONTENT_AUTHORITY);
                swipeRefreshLayout.setRefreshing(syncActive || syncPending);
            }
        });
    }

    @Override
    public void onRefresh() {
        syncUtils.syncGroups();
    }
}
