package com.mirfatif.privtasks;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Process;
import com.mirfatif.privtasks.hiddenapis.HiddenAPIs;
import com.mirfatif.privtasks.hiddenapis.HiddenAPIs.HiddenAPIsCallback;
import com.mirfatif.privtasks.hiddenapis.HiddenAPIsError;
import com.mirfatif.privtasks.hiddenapis.HiddenAPIsException;
import com.mirfatif.privtasks.hiddenapis.HiddenAPIsImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrivTasks {

  private static final String TAG = "PrivTaks";

  private final HiddenAPIs mHiddenAPIs;
  private final PrivTasksCallback mCallback;

  public PrivTasks(PrivTasksCallback callback) {
    mHiddenAPIs = new HiddenAPIsImpl(new HiddenAPIsCallbackImpl());
    mCallback = callback;
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// APP OPS /////////////////////////////
  //////////////////////////////////////////////////////////////////

  private int NUM_OP = -1;

  public Integer getNumOps() throws HiddenAPIsError {
    if (NUM_OP == -1) {
      NUM_OP = mHiddenAPIs.getNumOps();
    }
    return NUM_OP;
  }

  public List<Integer> buildOpToDefaultModeList() throws HiddenAPIsError {
    Integer opNum = getNumOps();
    if (opNum == null) {
      return null;
    }
    List<Integer> opToDefModeList = new ArrayList<>();
    for (int i = 0; i < opNum; i++) {
      opToDefModeList.add(mHiddenAPIs.opToDefaultMode(i));
    }
    return opToDefModeList;
  }

  public List<Integer> buildOpToSwitchList() throws HiddenAPIsError {
    Integer opNum = getNumOps();
    if (opNum == null) {
      return null;
    }
    List<Integer> opToSwitchList = new ArrayList<>();
    for (int i = 0; i < opNum; i++) {
      opToSwitchList.add(mHiddenAPIs.opToSwitch(i));
    }
    return opToSwitchList;
  }

  public List<String> buildOpToNameList() throws HiddenAPIsError {
    Integer opNum = getNumOps();
    if (opNum == null) {
      return null;
    }
    List<String> appOpsList = new ArrayList<>();
    for (int i = 0; i < opNum; i++) {
      appOpsList.add(mHiddenAPIs.opToName(i));
    }
    return appOpsList;
  }

  public List<String> buildModeToNameList() throws HiddenAPIsError {
    List<String> appOpsModes = new ArrayList<>();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      for (int i = 0; i < mHiddenAPIs.getOpModeNamesSize(); i++) {
        appOpsModes.add(mHiddenAPIs.modeToName(i));
      }
    } else {
      appOpsModes = Arrays.asList("allow", "ignore", "deny", "default");
    }
    return appOpsModes;
  }

  public List<String> buildPermToOpCodeList(PackageManager pm) throws HiddenAPIsError {
    List<String> permToOpCodeList = new ArrayList<>();
    List<String> permGroupsList = new ArrayList<>();
    if (pm != null) {
      for (PermissionGroupInfo pgi : pm.getAllPermissionGroups(0)) {
        permGroupsList.add(pgi.name);
      }
    } else {
      try {
        for (Object pgi : mHiddenAPIs.getPermGroupInfoList()) {
          permGroupsList.add(((PermissionGroupInfo) pgi).name);
        }
      } catch (HiddenAPIsException e) {
        mCallback.sendRequest(Commands.GET_PERM_GRP_INFO_LIST_FAILED);
        e.printStackTrace();
      }
    }
    permGroupsList.add(null);

    List<PermissionInfo> permInfoList = new ArrayList<>();
    for (String permGroup : permGroupsList) {
      if (pm != null) {
        try {
          permInfoList = pm.queryPermissionsByGroup(permGroup, 0);
        } catch (NameNotFoundException e) {
          mCallback.logE(TAG + ": buildPermToOpCodeList: " + e.toString());
        }
      } else {
        try {
          for (Object object : mHiddenAPIs.getPermInfoList(permGroup)) {
            permInfoList.add((PermissionInfo) object);
          }
        } catch (HiddenAPIsException e) {
          e.printStackTrace();
        }
      }
    }

    for (PermissionInfo permInfo : permInfoList) {
      if (!permInfo.packageName.equals("android")) {
        continue;
      }
      int opCode = mHiddenAPIs.permissionToOpCode(permInfo.name);
      if (opCode != mHiddenAPIs.getOpNone()) {
        permToOpCodeList.add(permInfo.name + ":" + opCode);
      }
    }
    return permToOpCodeList;
  }

  public void setAppOpsMode(String[] args) throws HiddenAPIsError {
    if (!haveWrongArgs(args, 4)) {
      int op = mHiddenAPIs.strDebugOpToOp(args[1]);
      int uid = Integer.parseInt(args[2]);
      String pkgName = args[3];
      int mode = Integer.parseInt(args[4]);

      try {
        if (pkgName.equals("null")) {
          mHiddenAPIs.setUidMode(op, uid, mode);
        } else {
          mHiddenAPIs.setMode(op, uid, pkgName, mode);
        }
      } catch (HiddenAPIsException e) {
        mCallback.sendRequest(Commands.SET_APP_OPS_MODE_FAILED);
        e.printStackTrace();
      }
    }
  }

  public void resetAppOps(String[] args) {
    if (!haveWrongArgs(args, 2)) {
      try {
        mHiddenAPIs.resetAllModes(Integer.parseInt(args[1]), args[2]);
      } catch (HiddenAPIsException e) {
        mCallback.sendRequest(Commands.RESET_APP_OPS_FAILED);
        e.printStackTrace();
      }
    }
  }

  public List<MyPackageOps> getOpsForPackage(String[] args) {
    if (haveWrongArgs(args, 3, false)) {
      return null;
    }
    return getMyPackageOpsList(Integer.parseInt(args[1]), args[2], args[3]);
  }

  public List<MyPackageOps> getMyPackageOpsList(int uid, String packageName, String op)
      throws HiddenAPIsError {
    try {
      return mHiddenAPIs.getMyPackageOpsList(uid, packageName, op, getNumOps());
    } catch (HiddenAPIsException e) {
      rateLimitThrowable(e);
      return null;
    }
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////// MANIFEST PERMISSIONS //////////////////////
  //////////////////////////////////////////////////////////////////

  public Integer getPermissionFlags(String[] args) {
    if (haveWrongArgs(args, 3, false)) {
      return null;
    }
    try {
      return mHiddenAPIs.getPermissionFlags(args[1], args[2], Integer.parseInt(args[3]));
    } catch (HiddenAPIsException e) {
      rateLimitThrowable(e);
      return null;
    }
  }

  public void grantRevokePermission(boolean grant, String[] args) {
    if (!haveWrongArgs(args, 3)) {
      try {
        if (grant) {
          mHiddenAPIs.grantRuntimePermission(args[1], args[2], Integer.parseInt(args[3]));
        } else {
          mHiddenAPIs.revokeRuntimePermission(args[1], args[2], Integer.parseInt(args[3]));
        }
      } catch (HiddenAPIsException e) {
        mCallback.sendRequest(grant ? Commands.GRANT_PERM_FAILED : Commands.REVOKE_PERM_FAILED);
        e.printStackTrace();
      }
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// PACKAGES ////////////////////////////
  //////////////////////////////////////////////////////////////////

  public void setAppEnabledState(boolean enable, String[] args) {
    if (!haveWrongArgs(args, 2)) {
      String pkg = args[1];
      int userId = Integer.parseInt(args[2]);
      String callingPkg = "shell:" + Process.myUid();

      int state;
      if (enable) {
        state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
      } else {
        state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
      }

      try {
        mHiddenAPIs.setApplicationEnabledSetting(pkg, state, 0, userId, callingPkg);
      } catch (HiddenAPIsException e) {
        mCallback.sendRequest(enable ? Commands.ENABLE_PKG_FAILED : Commands.DISABLE_PKG_FAILED);
        e.printStackTrace();
      }
    }
  }

  public List<MyPackageInfo> getInstalledPackages(String[] args) {
    if (haveWrongArgs(args, 2)) {
      return null;
    }

    int flags = Integer.parseInt(args[1]), userId = Integer.parseInt(args[2]);

    try {
      List<MyPackageInfo> myPkgInfoList = new ArrayList<>();
      for (Object object : mHiddenAPIs.getInstalledPackages(flags, userId)) {
        PackageInfo pkgInfo = (PackageInfo) object;
        MyPackageInfo myPkgInfo = new MyPackageInfo();
        myPkgInfo.packageName = pkgInfo.packageName;
        myPkgInfo.requestedPermissionsFlags = pkgInfo.requestedPermissionsFlags;
        myPkgInfo.uid = pkgInfo.applicationInfo.uid;
        myPkgInfo.enabled = pkgInfo.applicationInfo.enabled;
        myPkgInfoList.add(myPkgInfo);
      }
      return myPkgInfoList;
    } catch (HiddenAPIsException e) {
      mCallback.sendRequest(Commands.GET_INSTALLED_PKGS_FAILED);
      e.printStackTrace();
      return null;
    }
  }

  public MyPackageInfo getPkgInfo(String[] args) {
    if (haveWrongArgs(args, 3)) {
      return null;
    }

    String pkgName = args[1];
    int flags = Integer.parseInt(args[2]), userId = Integer.parseInt(args[3]);

    PackageInfo pkgInfo;
    try {
      pkgInfo = mHiddenAPIs.getPkgInfo(pkgName, flags, userId);
    } catch (HiddenAPIsException e) {
      mCallback.sendRequest(Commands.GET_PKG_INFO_FAILED);
      e.printStackTrace();
      return null;
    }

    if (pkgInfo == null) {
      return null;
    }

    MyPackageInfo myPkgInfo = new MyPackageInfo();
    myPkgInfo.packageName = pkgInfo.packageName;
    myPkgInfo.requestedPermissionsFlags = pkgInfo.requestedPermissionsFlags;
    myPkgInfo.uid = pkgInfo.applicationInfo.uid;
    myPkgInfo.enabled = pkgInfo.applicationInfo.enabled;
    return myPkgInfo;
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// OTHERS ////////////////////////////
  //////////////////////////////////////////////////////////////////

  public int[] getPidsForCommands(String[] commands) {
    return mHiddenAPIs.getPidsForCommands(commands);
  }

  public void openAppInfo(String[] args) {
    if (!haveWrongArgs(args, 2)) {
      String pkgName = args[1];
      int userId = Integer.parseInt(args[2]);

      try {
        int res = mHiddenAPIs.openAppInfo(pkgName, userId);
        if (res != getAmSuccessCode()) {
          mCallback.logE(TAG + ": openAppInfo: result code: " + res);
        }
      } catch (HiddenAPIsException e) {
        mCallback.sendRequest(Commands.OPEN_APP_INFO_FAILED);
        e.printStackTrace();
      }
    }
  }

  private int getAmSuccessCode() {
    try {
      return mHiddenAPIs.getAmSuccessCode();
    } catch (HiddenAPIsError e) {
      mCallback.logE(TAG + ": getAmSuccessCode: " + e.toString());
      return 0;
    }
  }

  public void sendRequest(String command, String codeWord) {
    try {
      mHiddenAPIs.sendRequest(command, mCallback.getAppUserId(), codeWord);
    } catch (HiddenAPIsException e) {
      e.printStackTrace();
    }
  }

  public List<String> getUsers() {
    try {
      return mHiddenAPIs.getUsers();
    } catch (HiddenAPIsException e) {
      mCallback.sendRequest(Commands.GET_USERS_FAILED);
      e.printStackTrace();
      return null;
    }
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////// COMMON METHODS /////////////////////////
  //////////////////////////////////////////////////////////////////

  public boolean canUseIAppOpsService() {
    return mHiddenAPIs.canUseIAppOpsService();
  }

  public boolean canUseIPm() {
    return mHiddenAPIs.canUseIPm();
  }

  private boolean haveWrongArgs(String[] cmd, int count) {
    return haveWrongArgs(cmd, count, true);
  }

  private boolean haveWrongArgs(String[] cmd, int count, boolean showToast) {
    if (cmd.length == count + 1) {
      return false;
    }
    if (showToast) {
      mCallback.sendRequest(Commands.WRONG_ARGS_RECEIVED);
    }
    mCallback.logE(TAG + ": Bad command: " + Arrays.toString(cmd));
    return true;
  }

  private long lastThrowableTimestamp = 0;

  private void rateLimitThrowable(Throwable t) {
    if (!mCallback.isDebug() && System.currentTimeMillis() - lastThrowableTimestamp < 1000) {
      return;
    }
    t.printStackTrace();
    lastThrowableTimestamp = System.currentTimeMillis();
  }

  private long lastLogTimestamp = 0;

  private void rateLimitLog(String msg) {
    if (mCallback.isDebug()) {
      mCallback.logE(TAG + ": " + msg + " - " + System.nanoTime());
    } else if (System.currentTimeMillis() - lastLogTimestamp >= 1000) {
      mCallback.logE(TAG + ": " + msg);
      lastLogTimestamp = System.currentTimeMillis();
    }
  }

  private class HiddenAPIsCallbackImpl implements HiddenAPIsCallback {

    @Override
    public void onGetUidOpsNpException(Exception e) {
      // Hey Android! You are buggy.
      if (mCallback.isDebug()) {
        e.printStackTrace();
      } else {
        mCallback.logE(TAG + ": getMyPackageOpsList: " + e.toString());
      }
    }

    @Override
    public void onInvalidOpCode(int opCode, String pkgName) {
      rateLimitLog("getMyPackageOpsList: bad op: " + opCode + " for package: " + pkgName);
    }

    @Override
    public void logError(String msg) {
      mCallback.logE(TAG + ": " + msg);
    }
  }

  // Verbose logging
  public interface PrivTasksCallback {

    boolean isDebug();

    void logE(String msg);

    int getAppUserId();

    void sendRequest(String command);
  }
}
