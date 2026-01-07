package com.ttt.safevault;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ttt.safevault.crypto.CryptoManager;
import com.ttt.safevault.data.AppDatabase;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.security.SecurityConfig;
import com.ttt.safevault.security.SecurityManager;
import com.ttt.safevault.service.BackendServiceImpl;

/**
 * 服务定位器
 * 提供全局单例服务的访问点
 */
public class ServiceLocator {

    private static volatile ServiceLocator INSTANCE;

    private final Context applicationContext;
    private BackendService backendService;
    private SecurityManager securityManager;
    private SecurityConfig securityConfig;
    private CryptoManager cryptoManager;

    private ServiceLocator(@NonNull Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    public static void init(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (ServiceLocator.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServiceLocator(context);
                }
            }
        }
    }

    public static ServiceLocator getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ServiceLocator must be initialized first. Call init() in Application.onCreate()");
        }
        return INSTANCE;
    }

    /**
     * 获取后端服务
     */
    public BackendService getBackendService() {
        if (backendService == null) {
            synchronized (this) {
                if (backendService == null) {
                    backendService = new BackendServiceImpl(applicationContext);
                }
            }
        }
        return backendService;
    }

    /**
     * 获取安全管理器
     */
    public SecurityManager getSecurityManager() {
        if (securityManager == null) {
            synchronized (this) {
                if (securityManager == null) {
                    securityManager = new SecurityManager(applicationContext);
                }
            }
        }
        return securityManager;
    }

    /**
     * 获取安全配置
     */
    public SecurityConfig getSecurityConfig() {
        if (securityConfig == null) {
            synchronized (this) {
                if (securityConfig == null) {
                    securityConfig = new SecurityConfig(applicationContext);
                }
            }
        }
        return securityConfig;
    }

    /**
     * 获取加密管理器
     */
    public CryptoManager getCryptoManager() {
        if (cryptoManager == null) {
            synchronized (this) {
                if (cryptoManager == null) {
                    cryptoManager = new CryptoManager(applicationContext);
                }
            }
        }
        return cryptoManager;
    }

    /**
     * 获取应用上下文
     */
    public Context getApplicationContext() {
        return applicationContext;
    }
}
