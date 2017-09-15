package common.base.activitys;

import android.os.Bundle;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.listener.OnRecyclerItemClickEventListener;

import java.util.List;

import common.base.beans.BaseListEntity;
import common.base.interfaces.IRecyclerViewItemClickEventsListener;

/**
 * User: fee(1176610771@qq.com)
 * Date: 2016-05-16
 * Time: 18:06
 * DESC: 带有列表据的基类Activity 不指定布局,并且默认本基类统一处理单个列表的情况（该列表请求类型为requestTypeAboutListData）
 * 如果一个界面上有2个以上列表的网络请求，则子类可重载 dealWithBeyondListResponse以及dealWithBeyondListErrorResponse来做处理
 * 注：本类的<T,TListData> 其中的T指替父类的 T 表示网络请求需要返回的数据类型，TListData表示列表中需要的数据类型
 */
public abstract class BaseListActivity<T,TListData,VH extends BaseViewHolder> extends BaseNetCallActivity<T> implements IRecyclerViewItemClickEventsListener {
    // TODO: 2016/6/25 此处的适配器还可以想办法通用ListView的适配器
    protected BaseQuickAdapter<TListData,VH> adapter4RecyclerView;
    /**
     * 当前列表显示的
     */
    protected int curPage = 1;
    /**
     * 服务器上总共有的数据分布数
     */
    protected int totalPages = 1;
    /**
     * 每页的数据数量 默认为20条数据
     */
    protected int perPageDataCount = 20;
    /**
     * 即本列表的数据来源于哪个网络请求类型
     */
    protected int requestTypeAboutListData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (adapter4RecyclerView == null) {
            adapter4RecyclerView = getRecyclerViewAdapter();
            if (adapter4RecyclerView != null) {
                //deleted by fee 2016-12-16 该方法已被版本为2.6.6的BRVAH框架去除掉了，点击事件监听者转为RecyclerView设置
//                adapter4RecyclerView.setOnRecyclerViewItemClickListener(this);
            }
        }
        initNetDataListener();
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void dealWithErrorResponse(int curRequestDataType, String errorInfo) {
        if (curRequestDataType == requestTypeAboutListData) {
            listDataRequestFinish(adapter4RecyclerView.getJustDataCount()!= 0 ,errorInfo);
        }
        else{
            dealWithBeyondListErrorResponse(curRequestDataType, errorInfo);
        }
    }

    @Override
    protected void dealWithResponse(int requestDataType, T result) {
        if (requestDataType == requestTypeAboutListData) {
            //1、解析返回的数据成列表数据
            BaseListEntity<TListData> parsedListEntity = parseResponseResut(result);
            //2、从(含集合类的实体)中取出集合,并填充数据到适配器中
            if (parsedListEntity != null) {
                listDataRequestSuccess(parsedListEntity.getListData());
            }
            else{
                //modified/added by fee 2016-07-11 考虑：有时服务器的返回结果T result,并不能方便的解析成并转换成BaseListEntity的形式,为了通用，再增加一个方法
                //让子类直接转换成对应的集合数据
                listDataRequestSuccess(parseResponseToListData(result));
            }
            //3通知一次成功的请求完成
            listDataRequestFinish(adapter4RecyclerView.getJustDataCount() != 0,null);
        }
        else{
            dealWithBeyondListResponse(requestDataType, result);
        }
    }

    /**
     * 将服务器响应的结果转换成集合数据
     * totalPage的赋值可在此
     * @param result
     * @return
     */
    protected abstract List<TListData> parseResponseToListData(T result);

    /**
     * 列表数据的请求成功，该方法只负责处理把数据加载进Adapter
     * @param newDataFromNetWork 网络请求下来的数据
     */
    protected void listDataRequestSuccess(List<TListData> newDataFromNetWork) {
        if (newDataFromNetWork == null || newDataFromNetWork.isEmpty()) {
            return;
        }
        if (curPage == 1) {//仍然是在第一页请求数据，比如下拉刷新
            adapter4RecyclerView.clearData();
        }
        //处理可能重复的数据
        int newDataCount = newDataFromNetWork.size();
        for(int i = 0 ; i < newDataCount; i++) {
            if (compareData(adapter4RecyclerView.getData(), newDataFromNetWork.get(i))) {
                newDataFromNetWork.remove(i);
                i--;
            }
        }
        if (!newDataFromNetWork.isEmpty()) {
            adapter4RecyclerView.addData(newDataFromNetWork);
        }
    }
    /**
     * 子类可在此方法中初始化对应的适配器
     * @return
     */
    protected abstract BaseQuickAdapter<TListData,VH> getRecyclerViewAdapter();

    /***
     * 根据网络的响应类型解析成 BaseListEntity 实体对象
     * @param result
     * @return BaseListEntity object
     */
    protected abstract BaseListEntity<TListData> parseResponseResut(T result);
    /**
     * 由于各子类的数据比较为重复的判断条件不一样，所以交由子类具体实现
     * @param oldDatas
     * @param willAddedOne
     * @return true 表示新数据已经存在旧数据集合中了
     */
    protected abstract boolean compareData(List<TListData> oldDatas, TListData willAddedOne);
    /**
     * 列表数据请求结束(含网络异常情况下的请求结束)
     * marked by fee 2017-09-14:好象有点瑕疵，不方便子类如果上拉加载更多功能的情况
     * @param hasListDataResult 一次请求完成后加上上一次的数据最终是否有列表数据,true表示有数据，false表示没有
     * @param errorInfoIfRequestFail 如果是请求失败时的错误信息
     */
    protected abstract void listDataRequestFinish(boolean hasListDataResult, String errorInfoIfRequestFail);
    /**
     * 处理不是 获取列表数据的网络请求的结果
     * @param requestDataType
     * @param result
     */
    protected void dealWithBeyondListResponse(int requestDataType, T result) {

    }

    /**
     * 处理不是 获取列表数据的网络请求的错误
     * @param curRequestDataType
     * @param errorInfo
     */
    protected void dealWithBeyondListErrorResponse(int curRequestDataType, String errorInfo){
        super.dealWithErrorResponse(curRequestDataType, errorInfo);//如果子类也不处理交由基类统一处理
    }

    private OnRecyclerItemClickEventListener onRecyclerItemClickEventListener;

    /**
     * 子类Activity的列表控件RecyclerView调用mRecyclerView.addOnItemTouchListener(obtainTheRecyclerItemClickListen())
     * 则可重写
     * @return
     */
    protected OnRecyclerItemClickEventListener obtainTheRecyclerItemClickListen() {
        if (onRecyclerItemClickEventListener == null) {
            onRecyclerItemClickEventListener = new OnRecyclerItemClickEventListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    BaseListActivity.this.onItemClick(adapter, view, position);
                }

                /**
                 * callback method to be invoked when an item in this view has been
                 * click and held
                 *
                 * @param adapter
                 * @param view     The view whihin the AbsListView that was clicked
                 * @param position The position of the view int the adapter
                 * @return true if the callback consumed the long click ,false otherwise
                 */
                @Override
                public void onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                    BaseListActivity.this.onItemLongClick(adapter,view,position);
                }

                @Override
                public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                    BaseListActivity.this.onItemChildClick(adapter, view, position);
                }

                @Override
                public void onItemChildLongClick(BaseQuickAdapter adapter, View view, int position) {
                    BaseListActivity.this.onItemChildLongClick(adapter, view, position);
                }
            };
        }
        return onRecyclerItemClickEventListener;
    }
    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     *
     * @param adapter
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     */
    @Override
    public abstract void onItemClick(BaseQuickAdapter adapter, View view, int position);

    /**
     * callback method to be invoked when an item in this view has been
     * click and held
     *
     * @param adapter
     * @param view     The view whihin the AbsListView that was clicked
     * @param position The position of the view int the adapter
     * @return true if the callback consumed the long click ,false otherwise
     */
    @Override
    public void onItemLongClick(BaseQuickAdapter adapter, View view, int position) {

    }

    /**
     * RecyclerView中的item布局内各子View的点击事件
     *
     * @param adapter  当前的适配器
     * @param view     当前被点击的视图View，用id来switch区分
     * @param position 被点击的子View在RecyclerView中的位置
     */
    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {

    }

    /**
     * RecyclerView中的item布局内各子View的长按事件
     *
     * @param adapter  当前的适配器
     * @param view     当前被点击的视图View，用id来switch区分
     * @param position 被长按的子View在RecyclerView中的位置
     */
    @Override
    public void onItemChildLongClick(BaseQuickAdapter adapter, View view, int position) {

    }
}
