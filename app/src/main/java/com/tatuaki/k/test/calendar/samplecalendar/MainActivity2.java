package com.tatuaki.k.test.calendar.samplecalendar;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity2 extends Activity implements EasyPermissions.PermissionCallbacks {
    private final String TAG = MainActivity2.class.getSimpleName();

    GoogleAccountCredential mGoogleAccountCredential;
    private TextView mOutputText;
    private Button mCallApiButton;
    private Button mCallApiButton2;
    private Button mCallApiButton3;
    private Button mCallApiButton4;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR};

    private static final int EVENT_TYPE_SHORT = 1;
    private static final int EVENT_TYPE_LONG = 2;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        //     private static final String BUTTON_TEXT = "Call Google Calendar API";
        mCallApiButton.setText("Primary Google Calendar List");
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);

        mCallApiButton2 = new Button(this);
        mCallApiButton2.setText("Custom Calendar Creaete");
        mCallApiButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton2.setEnabled(false);
                mOutputText.setText("Custom Calendar Creaete");
                getResultsFromApi2();
                mCallApiButton2.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton2);

        mCallApiButton3 = new Button(this);
        mCallApiButton3.setText("EVENT_TYPE_SHORT Creaete");
        mCallApiButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton3.setEnabled(false);
                mOutputText.setText("EVENT_TYPE_SHORT Creaete");
                getResultsCreateEvent(EVENT_TYPE_SHORT);
                mCallApiButton3.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton3);

        mCallApiButton4 = new Button(this);
        mCallApiButton4.setText("EVENT_TYPE_LONG Creaete");
        mCallApiButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton4.setEnabled(false);
                mOutputText.setText("EVENT_TYPE_LONG Creaete");
                getResultsCreateEvent(EVENT_TYPE_LONG);
                mCallApiButton4.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton4);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText("Click the \'" + BUTTON_TEXT + "\' button to test the API.");
        activityLayout.addView(mOutputText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mGoogleAccountCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),
                Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());

        FirebaseCrash.log("Activity created");
        // FirebaseCrash.report(new Exception("My first Android non-fatal error"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "choosePermission");
            choosePermission();
        }
    }

    /**
     * 新規カレンダー作成
     */
    private void getResultsFromApi2() {
        Log.d(TAG, "getResultsFromApi2");
        if (!isGooglePlayServicesAvailable()) {
            // Google Play Services が無効
            acquireGooglePlayServices();
        } else if (mGoogleAccountCredential.getSelectedAccountName() == null) {
            // 有効な Google アカウントが選択されていない
            Log.d(TAG, "getResultsFromApi to chooseAccount");
            chooseAccount();
        } else if (!isDeviceOnline()) {
            // 端末がインターネットに接続されていない場合
            mOutputText.setText("No network connection available.");
        } else {
            Log.e(TAG, "getResultsFromApi to new MakeCustomCalendarTask");
            new MakeCustomCalendarTask(mGoogleAccountCredential, EVENT_TYPE_SHORT).execute();
        }
    }

    /**
     * 新規カレンダー作成
     */
    private void getResultsCreateEvent(int eventType) {
        Log.d(TAG, "getResultsCreateEvent eventType " + eventType);
        if (!isGooglePlayServicesAvailable()) {
            // Google Play Services が無効
            acquireGooglePlayServices();
        } else if (mGoogleAccountCredential.getSelectedAccountName() == null) {
            // 有効な Google アカウントが選択されていない
            Log.d(TAG, "getResultsCreateEvent to chooseAccount");
            chooseAccount();
        } else if (!isDeviceOnline()) {
            // 端末がインターネットに接続されていない場合
            mOutputText.setText("No network connection available.");
        } else {
            Log.e(TAG, "getResultsFromApi to new MakeRequestCreateEvent");
            new MakeRequestCreateEvent(mGoogleAccountCredential, eventType).execute();
        }
    }

    /**
     * Google Calendar API の呼び出しの事前条件を確認し、条件を満たしていればAPIを呼び出す。
     *
     * 事前条件：
     * - 有効な Google Play Services がインストールされていること
     * - 有効な Google アカウントが選択されていること
     * - 端末がインターネット接続可能であること
     *
     * 事前条件を満たしていない場合には、ユーザーに説明を表示する。
     */
    /**
     * カレンダー予定取得
     */
    private void getResultsFromApi() {
        Log.d(TAG, "getResultsFromApi");
        if (!isGooglePlayServicesAvailable()) {
            // Google Play Services が無効
            acquireGooglePlayServices();
        } else if (mGoogleAccountCredential.getSelectedAccountName() == null) {
            // 有効な Google アカウントが選択されていない
            Log.d(TAG, "getResultsFromApi to chooseAccount");
            chooseAccount();
        } else if (!isDeviceOnline()) {
            // 端末がインターネットに接続されていない場合
            mOutputText.setText("No network connection available.");
        } else {
            Log.e(TAG, "getResultsFromApi to new MakeRequestTask");
            new MakeRequestTask(mGoogleAccountCredential).execute();
        }
    }

    /**
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void choosePermission() {
        Log.d(TAG, "choosePermission");
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.READ_CALENDAR)) {
            // ダイアログを表示して、ユーザーに"GET_ACCOUNTS"パーミッションを要求する
            EasyPermissions.requestPermissions(
                    this, "READ_CALENDAR This app needs to access your Google account (via Contacts).",
                    1100,
                    Manifest.permission.READ_CALENDAR);
        }
    }

    /**
     * Google　Calendar API の認証情報を使用するGoogleアカウントを設定する。
     * <p>
     * 既にGoogleアカウント名が保存されていればそれを使用し、保存されていなければ、
     * Googleアカウントの選択ダイアログを表示する。
     * <p>
     * 認証情報を用いたGoogleアカウントの設定には、"GET_ACCOUNTS"パーミッションを
     * 必要とするため、必要に応じてユーザーに"GET_ACCOUNTS"パーミッションを要求する
     * ダイアログが表示する。
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        Log.d(TAG, "chooseAccount");
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            // SharedPreferencesから保存済みGoogleアカウントを取得する
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mGoogleAccountCredential.setSelectedAccountName(accountName);
                Log.d(TAG, "getResultsFromApi accountName " + accountName);
                getResultsFromApi();
            } else {
                // Googleアカウントの選択を表示する
                // GoogleAccountCredentialのアカウント選択画面を使用する
                startActivityForResult(mGoogleAccountCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                Log.d(TAG, "chooseAccount to startActivityForResult");
            }
        } else {
            // ダイアログを表示して、ユーザーに"GET_ACCOUNTS"パーミッションを要求する
            EasyPermissions.requestPermissions(
                    this, "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * アカウント選択や認証など、呼び出し先のActivityから戻ってきた際に呼び出される。
     *
     * @param requestCode Activityの呼び出し時に指定したコード
     * @param resultCode  呼び出し先のActivityでの処理結果を表すコード
     * @param data        呼び出し先のActivityでの処理結果のデータ
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode " + requestCode + " resultCode " + resultCode);
        // onActivityResult requestCode 1000 resultCode -1
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    Log.e(TAG, "onActivityResult to getResultsFromApi");
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mGoogleAccountCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Android 6.0 (API 23) 以降にて、実行時にパーミッションを要求した際の結果を受け取る。
     *
     * @param requestCode  requestPermissions(android.app.Activity, String, int, String[])
     *                     を呼び出した際に渡した　request code
     * @param permissions  要求したパーミッションの一覧
     * @param grantResults 要求したパーミッションに対する承諾結果の配列
     *                     PERMISSION_GRANTED または PERMISSION_DENIED　が格納される。
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * 要求したパーミッションがユーザーに承諾された際に、EasyPermissionsライブラリから呼び出される。
     *
     * @param requestCode 要求したパーミッションに関連した request code
     * @param list        要求したパーミッションのリスト
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * 要求したパーミッションがユーザーに拒否された際に、EasyPermissionsライブラリから呼び出される。
     *
     * @param requestCode 要求したパーミッションに関連した request code
     * @param list        要求したパーミッションのリスト
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * 現在、端末がネットワークに接続されているかを確認する。
     *
     * @return ネットワークに接続されている場合にはtrueを、そうでない場合にはfalseを返す。
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * 端末に Google Play Services がインストールされ、アップデートされているか否かを確認する。
     *
     * @return 利用可能な Google Play Services がインストールされ、アップデートされている場合にはtrueを、
     * そうでない場合にはfalseを返す。
     */
    private boolean isGooglePlayServicesAvailable() {
        Log.d(TAG, "isGooglePlayServicesAvailable");
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * ユーザーにダイアログを表示して、Google Play Services を利用可能な状態に設定するように促す。
     * ただし、ユーザーが解決できないようなエラーの場合には、ダイアログを表示しない。
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * 有効な Google Play Services が見つからないことをエラーダイアログで表示する。
     *
     * @param connectionStatusCode Google Play Services が無効であることを示すコード
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(MainActivity2.this, connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * 非同期で　Google Calendar API の呼び出しを行うクラス。
     * <p>
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            Log.d(TAG, "MakeRequestTask");
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("com.tatuaki.k.test.calendar.samplecalendar").build();
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            Log.d(TAG, "doInBackground");
            try {
                Log.d(TAG, "doInBackground return getDataFromApi");
                return getDataFromApi();
            } catch (Exception e) {
                Log.e(TAG, "doInBackground " + e.getMessage());
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         *
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            Log.d(TAG, "getDataFromApi");
            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            //
            Log.e(TAG, "getDataFromApi now " + now);
            // DateTime end = new DateTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24時間後を保持する変数
            List<String> eventStrings = new ArrayList<String>();
            // 個々のイベントにアクセスするには、これで得られるイベントIDを使う。
            // デフォルトのカレンダーを対象にする場合は、カレンダーIDに固定の文字列’primary’を指定すればOK。
            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    //.setTimeZone("Asia/Tokyo")
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            int length = items.size();
            Log.w(TAG, "getDataFromApi length " + length);
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                    Log.w(TAG, "getDataFromApi event " + event.toString());
                }
                eventStrings.add(String.format("%s (%s)", event.getSummary(), start));
            }
            return eventStrings;
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            Log.d(TAG, "onPreExecute");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled");
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(((GooglePlayServicesAvailabilityIOException) mLastError)
                            .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(((UserRecoverableAuthIOException) mLastError).getIntent(), MainActivity2.REQUEST_AUTHORIZATION);
                } else {
                    // TODO ここにくる
                    mOutputText.setText("The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

    /***********************************************************************************************************************/
    /**
     * 非同期で　Google Calendar API の呼び出しを行うクラス。
     * <p>
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeCustomCalendarTask extends AsyncTask<Void, Void, String> {

        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        private int EVET_TYPE = EVENT_TYPE_SHORT;

        public MakeCustomCalendarTask(GoogleAccountCredential credential, int type) {
            Log.d(TAG, "MakeCustomCalendarTask type " + type);
            EVET_TYPE = type;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar
                    .Builder(transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Google Calendar API を呼び出すバックグラウンド処理。
         *
         * @param params 引数は不要
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                return createCalendar(EVET_TYPE);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * 選択されたGoogleアカウントに対して、新規にカレンダーを追加する。
         *
         * @return 作成したカレンダーのID
         * @throws IOException
         */
        private String createCalendar(int eventType) throws IOException {
            Log.d(TAG, "createCalendar");

            // カレンダーID取得
            SharedPreferences preferences = getSharedPreferences("Calendar", Context.MODE_PRIVATE);
            String calendarId = preferences.getString("★Sample", null);

            if (TextUtils.isEmpty(calendarId)) {
                // 新規にカレンダーを作成する
                com.google.api.services.calendar.model.Calendar calendar = new Calendar();
                // カレンダーにタイトルを設定する
                calendar.setSummary("★Sample");
                // カレンダーにタイムゾーンを設定する
                calendar.setTimeZone("Asia/Tokyo");
                // 作成したカレンダーをGoogleカレンダーに追加する
                Calendar createdCalendar = mService.calendars().insert(calendar).execute();
                calendarId = createdCalendar.getId();

                // カレンダー一覧から新規に作成したカレンダーのエントリを取得する
                CalendarListEntry calendarListEntry = mService.calendarList().get(calendarId).execute();
                // カレンダーのデフォルトの背景色を設定する
                calendarListEntry.setBackgroundColor("#ff0000");

                // カレンダーのデフォルトの背景色をGoogleカレンダーに反映させる
                CalendarListEntry updatedCalendarListEntry = mService.calendarList().update(calendarListEntry.getId(), calendarListEntry)
                        .setColorRgbFormat(true)
                        .execute();
                // 新規に作成したカレンダーのIDを返却する

                // TODO カレンダ^ID保存
                preferences = getSharedPreferences("Calendar", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("★Sample", calendarId);
                editor.commit();
            } else {
                Log.w(TAG, "createCalendar 存在");
            }
            Log.w(TAG, "createCalendar calendarId " + calendarId);
            return calendarId;
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            if (output == null || output.isEmpty()) {
                mOutputText.setText("No results returned.");
            } else {
                mOutputText.setText("Calendar created using the Google Calendar API: " + output);
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity2.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

    /***********************************************************************************************************************/
    /**
     * 非同期で　Google Calendar API の呼び出しを行うクラス。 3
     * <p>
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestCreateEvent extends AsyncTask<Void, Void, String> {

        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        private int EVET_TYPE = EVENT_TYPE_SHORT;

        public MakeRequestCreateEvent(GoogleAccountCredential credential, int eventType) {
            EVET_TYPE = eventType;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Google Calendar API を呼び出すバックグラウンド処理。
         *
         * @param params 引数は不要
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                return setEventCalendar(EVET_TYPE);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * カレンダーイベント新規作成
         *
         * @throws IOException
         */
        public String apply(com.google.api.services.calendar.Calendar client, String calendarId, Date startDate, Date endDate, String colorId) throws IOException {
            Event event = new Event();

            event.setSummary("★Event");
            event.setDescription("<a href=calendarSample://test/sample>to app launch!</a>" + " calendarId " + calendarId);
            event.setColorId(colorId);

            DateTime start = new DateTime(startDate, TimeZone.getTimeZone("Asia/Tokyo"));
            event.setStart(new EventDateTime().setDateTime(start));
            DateTime end = new DateTime(endDate, TimeZone.getTimeZone("Asia/Tokyo"));
            event.setEnd(new EventDateTime().setDateTime(end));

            // TODO 新規イベント作成
            Event createdEvent = client.events().insert(calendarId, event).execute();
            Log.d(TAG, "setEventCalendar createdEvent.getId()" + createdEvent.getId());
            return createdEvent.getId();
        }

        /**
         * 選択されたGoogleアカウントに対して、新規にカレンダーを追加する。
         *
         * @return 作成したカレンダーのID
         * @throws IOException
         */
        private String setEventCalendar(int eventType) throws IOException {
            Log.d(TAG, "setEventCalendar eventType " + eventType);

            // カレンダーID取得
            SharedPreferences preferences = getSharedPreferences("Calendar", Context.MODE_PRIVATE);
            String calendarId = preferences.getString("★Sample", null);

            if (TextUtils.isEmpty(calendarId)) {
                Log.d(TAG, "setEventCalendar 新規にカレンダーを作成");
                // 新規にカレンダーを作成する
                com.google.api.services.calendar.model.Calendar calendar = new Calendar();
                // カレンダーにタイトルを設定する
                calendar.setSummary("★Sample");
                // カレンダーにタイムゾーンを設定する
                calendar.setTimeZone("Asia/Tokyo");

                // 作成したカレンダーをGoogleカレンダーに追加する
                Calendar createdCalendar = mService.calendars().insert(calendar).execute();
                calendarId = createdCalendar.getId();
                // TODO カレンダ^ID保存
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("★Sample", calendarId);
                editor.commit();
            } else {
                Log.d(TAG, "setEventCalendar カレンダー存在");
            }
            Log.w(TAG, "calendarId " + calendarId);

            String eventId = null;
            if (eventType == EVENT_TYPE_SHORT) {
                Date startDate = new Date(System.currentTimeMillis());
                // Date endDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24時間後を保持する変数
                Date endDate = new Date(System.currentTimeMillis() + 60 * 60 * 1000); // 1時間後を保持する変数
                Log.e(TAG, "getDataFromApi startDate " + startDate + " endDate " + endDate);
                eventId = apply(mService, calendarId, startDate, endDate, "2");
            } else {
                Date startDate = new Date(System.currentTimeMillis());
                // Date endDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24時間後を保持する変数
                Date endDate = new Date(System.currentTimeMillis() + 24 * 3 * 60 * 60 * 1000); // 三日間
                Log.e(TAG, "getDataFromApi startDate " + startDate + " endDate " + endDate);
                eventId = apply(mService, calendarId, startDate, endDate, "5");
            }

            Log.e(TAG, "setEventCalendar eventId " + eventId);
            // カレンダーリストを返却する

            String list = showList(calendarId);
            return list;
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String calendarList) {
            Log.e(TAG, "onPostExecute calendarList " + calendarList);
            mProgress.hide();
            if (calendarList == null || calendarList.isEmpty()) {
                mOutputText.setText("No results returned.");
            } else {
                mOutputText.setText("Calendar calendarList::\n " + calendarList);
            }
        }

        private String showList(String calendarId) {
            DateTime now = new DateTime(System.currentTimeMillis());
            Log.e(TAG, "showList calendarId " + calendarId + " now " + now);
            // DateTime end = new DateTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24時間後を保持する変数
            List<String> eventStrings = new ArrayList<String>();
            try {
                Events events = mService.events().list(calendarId)
                        .setMaxResults(10)
                        .setTimeMin(now)
                        .setTimeZone("Asia/Tokyo")
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();
                List<Event> items = events.getItems();

                int length = items.size();
                Log.w(TAG, "showList length " + length);
                for (Event event : items) {
                    DateTime start = event.getStart().getDateTime();
                    if (start == null) {
                        start = event.getStart().getDate();
                    }
                    DateTime end = event.getEnd().getDateTime();
                    if (end == null) {
                        end = event.getEnd().getDate();
                    }
                    Log.w(TAG, "showList start " + start + " end " + end);
                    eventStrings.add(String.format("%s \n (start %s) \n (end %s)", event.getSummary(), start, end));
                    // eventStrings.add(String.format("%s (%s)", event.toString(), start));
                }
            } catch (Exception e) {
                Log.w(TAG, "showList Exception " + e.getMessage());
            }
            String listData = TextUtils.join("\n", eventStrings);
            Log.w(TAG, "showList listData " + listData);
            return listData;
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity2.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}