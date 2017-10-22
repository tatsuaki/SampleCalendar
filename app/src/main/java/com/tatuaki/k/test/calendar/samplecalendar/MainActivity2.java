package com.tatuaki.k.test.calendar.samplecalendar;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
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
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR};
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
        mCallApiButton.setText("Call Google Calendar API ListGet");
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
        mCallApiButton2.setText("Calendar Creaete");
        mCallApiButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton2.setEnabled(false);
                mOutputText.setText("Calendar Creaete");
                getResultsFromApi2();
                mCallApiButton2.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton2);

        mCallApiButton3 = new Button(this);
        mCallApiButton3.setText("Creaete Event");
        mCallApiButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton3.setEnabled(false);
                mOutputText.setText("Creaete Event");
                getResultsFromApi3();
                mCallApiButton3.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton3);

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
            Log.e(TAG, "getResultsFromApi to new MakeRequestTask2");
            new MakeRequestTask2(mGoogleAccountCredential).execute();
        }
    }

    /**
     * 新規カレンダー作成
     */
    private void getResultsFromApi3() {
        Log.d(TAG, "getResultsFromApi3");
        if (!isGooglePlayServicesAvailable()) {
            // Google Play Services が無効
            acquireGooglePlayServices();
        } else if (mGoogleAccountCredential.getSelectedAccountName() == null) {
            // 有効な Google アカウントが選択されていない
            Log.d(TAG, "getResultsFromApi3 to chooseAccount");
            chooseAccount();
        } else if (!isDeviceOnline()) {
            // 端末がインターネットに接続されていない場合
            mOutputText.setText("No network connection available.");
        } else {
            Log.e(TAG, "getResultsFromApi to new MakeRequestTask3");
            new MakeRequestTask3(mGoogleAccountCredential).execute();
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
     *
     * 既にGoogleアカウント名が保存されていればそれを使用し、保存されていなければ、
     * Googleアカウントの選択ダイアログを表示する。
     *
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
     *
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
     *
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask2 extends AsyncTask<Void, Void, String> {

        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        public MakeRequestTask2(GoogleAccountCredential credential) {
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
                return createCalendar();
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
        private String createCalendar() throws IOException {
            Log.d(TAG, "createCalendar");
            // 新規にカレンダーを作成する
            com.google.api.services.calendar.model.Calendar calendar = new Calendar();
            // カレンダーにタイトルを設定する
            calendar.setSummary("EventSampleCalendar");
            // カレンダーにタイムゾーンを設定する
            calendar.setTimeZone("Asia/Tokyo");

            // 作成したカレンダーをGoogleカレンダーに追加する
            Calendar createdCalendar = mService.calendars().insert(calendar).execute();
            String calendarId = createdCalendar.getId();
            Log.w(TAG, "calendarId " + calendarId);
            // カレンダー一覧から新規に作成したカレンダーのエントリを取得する
            CalendarListEntry calendarListEntry = mService.calendarList().get(calendarId).execute();

            // カレンダーのデフォルトの背景色を設定する
            calendarListEntry.setBackgroundColor("#ff0000");

            // カレンダーのデフォルトの背景色をGoogleカレンダーに反映させる
            CalendarListEntry updatedCalendarListEntry =
                    mService.calendarList().update(calendarListEntry.getId(), calendarListEntry)
                            .setColorRgbFormat(true)
                            .execute();
            // 新規に作成したカレンダーのIDを返却する
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
     * 非同期で　Google Calendar API の呼び出しを行うクラス。
     *
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask3 extends AsyncTask<Void, Void, String> {

        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        public MakeRequestTask3(GoogleAccountCredential credential) {
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
                return setEventCalendar();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }
        /*
         * カレンダーIDで予定を取得
         */
        public Events getEvents(String calendarId, com.google.api.services.calendar.Calendar client, Date startDate,
                                Date endDate) throws IOException {
            DateTime start = new DateTime(startDate,
                    TimeZone.getTimeZone("Asia/Tokyo"));
            DateTime end = new DateTime(endDate, TimeZone.getTimeZone("Asia/Tokyo"));
            com.google.api.services.calendar.Calendar.Events.List calendar = client.events().list(calendarId);
            calendar.setTimeMin(start);
            calendar.setTimeMax(end);
            calendar.setTimeZone("Asia/Tokyo");
            calendar.setOrderBy("startTime");
            calendar.setSingleEvents(true);

            Events events = calendar.execute();
            return events;
        }

        /**
         * カレンダーイベント新規作成
         * @throws IOException
         */
        public String apply(com.google.api.services.calendar.Calendar client, String calendarId, Date startDate, Date endDate) throws IOException{
            Event event = new Event();

            event.setSummary("sampleEvent");
            event.setDescription("calendarId " + calendarId);
            event.setColorId("2");

            DateTime start = new DateTime(startDate, TimeZone.getTimeZone("Asia/Tokyo"));
            event.setStart(new EventDateTime().setDateTime(start));
            DateTime end = new DateTime(endDate, TimeZone.getTimeZone("Asia/Tokyo"));
            event.setEnd(new EventDateTime().setDateTime(end));

            // TODO 新規イベント作成
            Event createdEvent = client.events().insert(calendarId, event).execute();
            return createdEvent.getId();
        }

        /**
         * カレンダーイベント変更
         *
         * eventをnewせず取得してカレンダーIDとイベントIDを指定してupdateすればOK
         * https://developers.google.com/google-apps/calendar/v3/reference/events/update?hl=ja
         * @throws IOException
         */
        public String update(com.google.api.services.calendar.Calendar client, String calendarId, String eventId,
                             Date startDate, Date endDate) throws IOException{
            Event event = client.events().get(calendarId, eventId).execute();
            if(event == null)  {
                return null;
            }
            event.setSummary("タイトル");
            event.setDescription("詳細");
            event.setColorId("2");

            DateTime start = new DateTime(startDate, TimeZone.getTimeZone("Asia/Tokyo"));
            event.setStart(new EventDateTime().setDateTime(start));
            DateTime end = new DateTime(endDate, TimeZone.getTimeZone("Asia/Tokyo"));
            event.setEnd(new EventDateTime().setDateTime(end));

            Event updatedEvent = client.events().update(calendarId, event.getId(), event).execute();

            return updatedEvent.getId();
        }
        /**
         * 選択されたGoogleアカウントに対して、新規にカレンダーを追加する。
         *
         * @return 作成したカレンダーのID
         * @throws IOException
         */
        private String setEventCalendar() throws IOException {
            Log.d(TAG, "setEventCalendar");
            // 新規にカレンダーを作成する
            com.google.api.services.calendar.model.Calendar calendar = new Calendar();
            // カレンダーにタイトルを設定する
            calendar.setSummary("EventSampleCalendar");
            // カレンダーにタイムゾーンを設定する
            calendar.setTimeZone("Asia/Tokyo");

            // 作成したカレンダーをGoogleカレンダーに追加する
            Calendar createdCalendar = mService.calendars().insert(calendar).execute();
            String calendarId = createdCalendar.getId();
            Log.w(TAG, "calendarId " + calendarId);

            // (com.google.api.services.calendar.Calendar client, String calendarId,Date startDate, Date endDate)

            Date startDate = new Date(System.currentTimeMillis());
            // Date endDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // 24時間後を保持する変数
            Date endDate = new Date(System.currentTimeMillis() + 60 * 60 * 1000); // 1時間後を保持する変数
            Log.e(TAG, "getDataFromApi startDate " + startDate + " endDate " + endDate);

            apply(mService, calendarId, startDate, endDate);
            // カレンダー一覧から新規に作成したカレンダーのエントリを取得する
//            CalendarListEntry calendarListEntry = mService.calendarList().get(calendarId).execute();
//
//            // カレンダーのデフォルトの背景色を設定する
//            calendarListEntry.setBackgroundColor("#ff0000");
//
//            // カレンダーのデフォルトの背景色をGoogleカレンダーに反映させる
//            CalendarListEntry updatedCalendarListEntry =
//                    mService.calendarList().update(calendarListEntry.getId(), calendarListEntry)
//                            .setColorRgbFormat(true)
//                            .execute();
//            // 新規に作成したカレンダーのIDを返却する
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
}