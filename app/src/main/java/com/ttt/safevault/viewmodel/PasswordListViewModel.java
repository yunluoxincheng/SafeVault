package com.ttt.safevault.viewmodel;

import android.app.Application;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 密码列表页面的ViewModel
 * 负责管理和展示密码条目列表
 */
public class PasswordListViewModel extends AndroidViewModel {

    private final BackendService backendService;
    private final ExecutorService executor;

    // LiveData用于UI状态管理
    private final MutableLiveData<List<PasswordItem>> _passwordItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> _isSearching = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isEmpty = new MutableLiveData<>(false);

    public LiveData<List<PasswordItem>> passwordItems = _passwordItems;
    public LiveData<Boolean> isLoading = _isLoading;
    public LiveData<String> searchQuery = _searchQuery;
    public LiveData<Boolean> isSearching = _isSearching;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<Boolean> isEmpty = _isEmpty;

    private List<PasswordItem> allItems; // 保存原始数据

    public PasswordListViewModel(@NonNull Application application, BackendService backendService) {
        super(application);
        this.backendService = backendService;
        this.executor = Executors.newSingleThreadExecutor();
        loadPasswordItems();
    }

    /**
     * 加载所有密码条目
     */
    public void loadPasswordItems() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                List<PasswordItem> items = backendService.getAllItems();
                allItems = items;

                // 应用当前搜索过滤
                String currentQuery = _searchQuery.getValue();
                if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                    items = filterItems(items, currentQuery);
                }

                _passwordItems.postValue(items);
                _isEmpty.postValue(items.isEmpty());
            } catch (Exception e) {
                _errorMessage.postValue("加载失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 搜索密码条目
     */
    public void search(String query) {
        _searchQuery.setValue(query);

        if (query == null || query.trim().isEmpty()) {
            clearSearch();
            return;
        }

        _isSearching.setValue(true);

        executor.execute(() -> {
            try {
                List<PasswordItem> filteredItems;

                // 使用后端搜索（更安全，后端处理解密）
                filteredItems = backendService.search(query.trim());

                // 如果有原始数据，也可以在前端过滤（更快）
                if (allItems != null) {
                    filteredItems = filterItems(allItems, query.trim());
                }

                _passwordItems.postValue(filteredItems);
                _isEmpty.postValue(filteredItems.isEmpty());
            } catch (Exception e) {
                _errorMessage.postValue("搜索失败: " + e.getMessage());
            }
        });
    }

    /**
     * 清除搜索，显示所有条目
     */
    public void clearSearch() {
        _searchQuery.setValue("");
        _isSearching.setValue(false);

        if (allItems != null) {
            _passwordItems.setValue(allItems);
            _isEmpty.setValue(allItems.isEmpty());
        } else {
            loadPasswordItems();
        }
    }

    /**
     * 删除密码条目
     */
    public void deletePasswordItem(int itemId) {
        executor.execute(() -> {
            try {
                boolean success = backendService.deleteItem(itemId);
                if (success) {
                    // 从列表中移除
                    List<PasswordItem> currentItems = _passwordItems.getValue();
                    if (currentItems != null) {
                        currentItems.removeIf(item -> item.getId() == itemId);
                        _passwordItems.postValue(currentItems);

                        // 更新原始数据
                        if (allItems != null) {
                            allItems.removeIf(item -> item.getId() == itemId);
                        }
                    }
                } else {
                    _errorMessage.postValue("删除失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("删除失败: " + e.getMessage());
            }
        });
    }

    /**
     * 获取单个密码条目
     */
    public void getPasswordItem(int itemId, PasswordItemCallback callback) {
        executor.execute(() -> {
            try {
                PasswordItem item = backendService.decryptItem(itemId);
                if (callback != null) {
                    callback.onResult(item, null);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onResult(null, e);
                }
            }
        });
    }

    /**
     * 复制密码到剪贴板
     */
    public void copyPassword(int itemId) {
        executor.execute(() -> {
            try {
                PasswordItem item = backendService.decryptItem(itemId);
                if (item != null && item.getPassword() != null) {
                    // TODO: 使用剪贴板管理器复制密码
                    // clipboardManager.copy(item.getPassword());
                }
            } catch (Exception e) {
                _errorMessage.postValue("复制失败: " + e.getMessage());
            }
        });
    }

    /**
     * 刷新数据
     */
    public void refresh() {
        loadPasswordItems();
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        _errorMessage.setValue(null);
    }

    /**
     * 过滤条目（前端过滤）
     */
    private List<PasswordItem> filterItems(List<PasswordItem> items, String query) {
        return items.stream()
                .filter(item -> matchesQuery(item, query))
                .collect(Collectors.toList());
    }

    /**
     * 检查条目是否匹配搜索词
     */
    private boolean matchesQuery(PasswordItem item, String query) {
        String lowerQuery = query.toLowerCase();

        return (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerQuery)) ||
               (item.getUsername() != null && item.getUsername().toLowerCase().contains(lowerQuery)) ||
               (item.getUrl() != null && item.getUrl().toLowerCase().contains(lowerQuery)) ||
               (item.getNotes() != null && item.getNotes().toLowerCase().contains(lowerQuery));
    }

    /**
     * 回调接口
     */
    public interface PasswordItemCallback {
        void onResult(PasswordItem item, Exception error);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}