package com.mirfatif.privtasks.hiddenapis;

import static com.mirfatif.privtasks.Commands.APP_PKG_NAME;
import static com.mirfatif.privtasks.Commands.CMD_RCV_SVC;

import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IUserManager;
import android.os.IUserManager.Stub;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.permission.IPermissionManager;
import android.provider.Settings;
import com.android.internal.app.IAppOpsService;
import com.mirfatif.privtasks.Commands;
import com.mirfatif.privtasks.MyPackageOps;
import java.util.ArrayList;
import java.util.List;

public class HiddenAPIsImpl extends HiddenAPIs {

  private IAppOpsService mIAppOpsService;
  private IPackageManager mIPackageManager;
  private IPermissionManager mIPermissionManager;
  private IUserManager mIUserManager;
  private IActivityManager mIActivityManager;

  public HiddenAPIsImpl(HiddenAPIsCallback callback) {
    super(callback);
    try {
      mIAppOpsService =
          IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
    }

    try {
      mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        mIPermissionManager =
            IPermissionManager.Stub.asInterface(ServiceManager.getService("permissionmgr"));
      } catch (NoSuchMethodError e) {
        e.printStackTrace();
      }
    }

    try {
      mIUserManager = Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE));
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Also: ActivityManager.getService()
        mIActivityManager =
            IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE));
      } else {
        mIActivityManager = ActivityManagerNative.getDefault();
      }
    } catch (NoSuchMethodError e) {
      e.printStackTrace();
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// APP OPS /////////////////////////////
  //////////////////////////////////////////////////////////////////

  @Override
  int _getNumOps() throws HiddenAPIsError {
    try {
      return AppOpsManager.getNumOps();
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public int getOpModeNamesSize() throws HiddenAPIsError {
    try {
      return AppOpsManager.MODE_NAMES.length;
    } catch (NoSuchFieldError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public int opToDefaultMode(int opCode) throws HiddenAPIsError {
    try {
      return AppOpsManager.opToDefaultMode(opCode);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public int opToSwitch(int opCode) throws HiddenAPIsError {
    try {
      return AppOpsManager.opToSwitch(opCode);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public String opToName(int opCode) throws HiddenAPIsError {
    try {
      return AppOpsManager.opToName(opCode);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public String modeToName(int opMode) throws HiddenAPIsError {
    try {
      return AppOpsManager.modeToName(opMode);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public int permissionToOpCode(String permName) throws HiddenAPIsError {
    try {
      return AppOpsManager.permissionToOpCode(permName);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public int strDebugOpToOp(String opName) throws HiddenAPIsError {
    try {
      return AppOpsManager.strDebugOpToOp(opName);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public void setMode(int op, int uid, String pkgName, int mode) throws HiddenAPIsException {
    try {
      mIAppOpsService.setMode(op, uid, pkgName, mode);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  public void setUidMode(int op, int uid, int mode) throws HiddenAPIsException {
    try {
      mIAppOpsService.setUidMode(op, uid, mode);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  public void resetAllModes(int userId, String pkgName) throws HiddenAPIsException {
    try {
      mIAppOpsService.resetAllModes(userId, pkgName);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  public List<MyPackageOps> getMyPackageOpsList(int uid, String packageName, String op, int opNum)
      throws HiddenAPIsException, HiddenAPIsError {
    try {
      return _getMyPackageOpsList(uid, packageName, op, opNum);
    } catch (RemoteException e) {
      throw new HiddenAPIsException(e);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  private List<MyPackageOps> _getMyPackageOpsList(int uid, String packageName, String op, int opNum)
      throws RemoteException, NoSuchMethodError {
    int[] ops = op.equals("null") ? null : new int[] {Integer.parseInt(op)};

    List<PackageOps> pkgOpsList = null;

    if (!packageName.equals("null")) {
      pkgOpsList = mIAppOpsService.getOpsForPackage(uid, packageName, ops);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        pkgOpsList = mIAppOpsService.getUidOps(uid, ops);
      } catch (NullPointerException e) {
        mCallback.onGetUidOpsNpException(e);
        return null;
      }
    }

    List<MyPackageOps> myPackageOpsList = new ArrayList<>();

    if (pkgOpsList == null) {
      return myPackageOpsList;
    }

    for (PackageOps packageOps : pkgOpsList) {
      MyPackageOps myPackageOps = new MyPackageOps();
      List<MyPackageOps.MyOpEntry> myOpEntryList = new ArrayList<>();

      for (OpEntry opEntry : packageOps.getOps()) {
        MyPackageOps.MyOpEntry myOpEntry = new MyPackageOps.MyOpEntry();

        myOpEntry.op = opEntry.getOp();

        if (myOpEntry.op >= opNum) {
          mCallback.onInvalidOpCode(myOpEntry.op, packageOps.getPackageName());
          continue;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          if (OP_FLAGS_ALL == null) {
            myOpEntry.lastAccessTime = -1;
          } else {
            myOpEntry.lastAccessTime = opEntry.getLastAccessTime(OP_FLAGS_ALL);
          }
        } else {
          myOpEntry.lastAccessTime = getTime(opEntry);
        }
        myOpEntry.opMode = opEntry.getMode();

        myOpEntryList.add(myOpEntry);
      }

      myPackageOps.packageName = packageOps.getPackageName();
      myPackageOps.myOpEntryList = myOpEntryList;

      myPackageOpsList.add(myPackageOps);
    }
    return myPackageOpsList;
  }

  private long getTime(OpEntry opEntry) {
    return opEntry.getTime();
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////// MANIFEST PERMISSIONS //////////////////////
  //////////////////////////////////////////////////////////////////

  public List<?> getPermGroupInfoList() throws HiddenAPIsException, HiddenAPIsError {
    try {
      ParceledListSlice<?> pls;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        pls = mIPermissionManager.getAllPermissionGroups(0);
      } else {
        pls = (ParceledListSlice<?>) mIPackageManager.getAllPermissionGroups(0);
      }
      List<?> pgiList = null;
      if (pls != null) {
        pgiList = pls.getList();
      }
      return pgiList != null ? pgiList : new ArrayList<>();
    } catch (RemoteException e) {
      throw new HiddenAPIsException(e);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public List<?> getPermInfoList(String permGroup) throws HiddenAPIsException, HiddenAPIsError {
    try {
      ParceledListSlice<?> pls;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        pls = mIPermissionManager.queryPermissionsByGroup(permGroup, 0);
      } else {
        pls = (ParceledListSlice<?>) mIPackageManager.queryPermissionsByGroup(permGroup, 0);
      }
      List<?> piList = null;
      if (pls != null) {
        piList = pls.getList();
      }
      return piList != null ? piList : new ArrayList<>();
    } catch (RemoteException e) {
      throw new HiddenAPIsException(e);
    } catch (NoSuchMethodError e) {
      throw new HiddenAPIsError(e);
    }
  }

  public int getPermissionFlags(String permName, String pkgName, int userId)
      throws HiddenAPIsException {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return mIPermissionManager.getPermissionFlags(permName, pkgName, userId);
      } else {
        return mIPackageManager.getPermissionFlags(permName, pkgName, userId);
      }
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  public void grantRuntimePermission(String pkgName, String permName, int userId)
      throws HiddenAPIsException {
    try {
      mIPackageManager.grantRuntimePermission(pkgName, permName, userId);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  public void revokeRuntimePermission(String pkgName, String permName, int userId)
      throws HiddenAPIsException {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        mIPermissionManager.revokeRuntimePermission(pkgName, permName, userId, null);
      } else {
        mIPackageManager.revokeRuntimePermission(pkgName, permName, userId);
      }
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  //////////////////////////////////////////////////////////////////
  //////////////////////////// PACKAGES ////////////////////////////
  //////////////////////////////////////////////////////////////////

  public void setApplicationEnabledSetting(
      String pkg, int state, int flags, int userId, String callingPkg) throws HiddenAPIsException {
    try {
      mIPackageManager.setApplicationEnabledSetting(pkg, state, flags, userId, callingPkg);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  @Override
  public List<?> getInstalledPackages(int flags, int userId) throws HiddenAPIsException {
    try {
      return mIPackageManager.getInstalledPackages(flags, userId).getList();
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  @Override
  public PackageInfo getPkgInfo(String pkgName, int flags, int userId) throws HiddenAPIsException {
    try {
      return mIPackageManager.getPackageInfo(pkgName, flags, userId);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  //////////////////////////////////////////////////////////////////
  ////////////////////////////// OTHERS ////////////////////////////
  //////////////////////////////////////////////////////////////////

  public int[] getPidsForCommands(String[] commands) {
    return Process.getPidsForCommands(commands);
  }

  public int openAppInfo(String pkgName, int userId) throws HiddenAPIsException {
    if (mIActivityManager == null) {
      throw new HiddenAPIsException("Could not initialize IActivityManager");
    }

    Intent intent =
        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:" + pkgName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    try {
      return mIActivityManager.startActivityAsUser(
          null, null, intent, null, null, null, 0, 0, null, null, userId);
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }
  }

  public void sendRequest(String command, int userId, String codeWord) throws HiddenAPIsException {
    Intent intent = new Intent(command).setClassName(APP_PKG_NAME, APP_PKG_NAME + CMD_RCV_SVC);
    intent.putExtra(Commands.CODE_WORD, codeWord);
    ComponentName cn;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        cn = mIActivityManager.startService(null, intent, null, false, APP_PKG_NAME, null, userId);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        cn = mIActivityManager.startService(null, intent, null, false, APP_PKG_NAME, userId);
      } else {
        cn = mIActivityManager.startService(null, intent, null, APP_PKG_NAME, userId);
      }
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    }

    if (cn == null || !cn.getPackageName().equals(APP_PKG_NAME)) {
      throw new HiddenAPIsException("Could not start DaemonToastSvc");
    }
  }

  private boolean isQPlus = true;

  public List<String> getUsers() throws HiddenAPIsException {
    if (mIUserManager == null) {
      throw new HiddenAPIsException("Could not initialize mIUserManager");
    }
    List<UserInfo> userInfoList;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isQPlus) {
        userInfoList = mIUserManager.getUsers(false, false, false);
      } else {
        userInfoList = mIUserManager.getUsers(false);
      }
    } catch (RemoteException | SecurityException e) {
      throw new HiddenAPIsException(e);
    } catch (NoSuchMethodError e) {
      if (isQPlus) {
        mCallback.logError("getUsers: " + e.toString());
        isQPlus = false;
        return getUsers();
      }
      throw new HiddenAPIsException(e);
    }
    List<String> userIds = new ArrayList<>();
    for (UserInfo userInfo : userInfoList) {
      userIds.add(userInfo.id + "|" + (userInfo.name != null ? userInfo.name : ""));
    }
    return userIds;
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////// COMMON METHODS /////////////////////////
  //////////////////////////////////////////////////////////////////

  public boolean canUseIAppOpsService() {
    return mIAppOpsService != null;
  }

  public boolean canUseIPm() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return mIPermissionManager != null;
    }
    return mIPackageManager != null;
  }
}
