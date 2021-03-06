package com.zeroone.conceal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.StringRes;

import com.facebook.crypto.CryptoConfig;
import com.zeroone.conceal.model.CryptoFile;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.zeroone.conceal.model.Constant.DEFAULT_DIRECTORY;
import static com.zeroone.conceal.model.Constant.DEFAULT_IMAGE_FOLDER;
import static com.zeroone.conceal.model.Constant.DEFAULT_MAIN_FOLDER;
import static com.zeroone.conceal.model.Constant.DEFAULT_PREFIX_FILENAME;

/**
 * @author : hafiq on 23/03/2017.
 */

public class ConcealPrefRepository {

    private Context mContext;
    private CryptoConfig mKeyChain = CryptoConfig.KEY_256;
    private boolean mEnabledCrypto = false;
    private boolean mEnableCryptKey = false;
    private String mEntityPasswordRaw = null;
    private static String mFolderName;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static ConcealCrypto concealCrypto;
    private static OnDataChangeListener onDataChangeListener;

    @SuppressLint("CommitPrefEdits")
    private ConcealPrefRepository(@NonNull PreferencesBuilder builder){
        mContext = builder.mContext.get();
        mKeyChain = builder.mKeyChain;
        mEnabledCrypto = builder.mEnabledCrypto;
        mEnableCryptKey = builder.mEnableCryptKey;
        mEntityPasswordRaw = builder.mEntityPasswordRaw;
        mFolderName = builder.mFolderName;
        sharedPreferences = builder.sharedPreferences;
        onDataChangeListener = builder.onDataChangeListener;

        //init editor
        editor = sharedPreferences.edit();

        //init crypto
        concealCrypto = new ConcealCrypto.CryptoBuilder(mContext)
                .createPassword(mEntityPasswordRaw)
                .setKeyChain(mKeyChain)
                .setEnableCrypto(mEnabledCrypto)
                .setEnableKeyCrypto(mEnableCryptKey)
                .setStoredFolder(mFolderName)
                .create();

        //init listener if set
        if (onDataChangeListener!=null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    onDataChangeListener.onDataChange(key, sharedPreferences.getString(key,""));
                }
            });
        }
    }

    /**********************
     * DESTROY FILES
     **********************/
    public void destroyCrypto(){
        concealCrypto.clearCrypto();
    }

    public void destroySharedPreferences(){
        editor.clear().apply();
        destroyCrypto();
    }


    /*******************************
     * GET SHAREDPREFERENCES TOTAL
     *******************************/
    public int getPrefsSize(){
        return getPreferences().getAll().size();
    }


    /*******************************
     * REMOVING KEYS
     *******************************/
    /* Remove by Key */
    public void remove(@NonNull String... keys){
        for (String key:keys){
            editor.remove(hashKey(key));
        }
        editor.apply();
    }


    /**
     * special cases for file to remove by key
     * @param key preferences key
     * @return boolean
     */
    public boolean removeFile(@NonNull String key){
        String path = getString(key);
        if (path != null) {
            File imagePath = new File(path);
            if (imagePath.exists()) {
                if (!imagePath.delete()) {
                    return false;
                }
                remove(key);
            }
            return true;
        }
        return false;
    }


    /**
     * get all encrypted file in created folder
     * @return @CryptoFile
     */
    public List<CryptoFile> getAllConcealEncryptedFiles(){
        return getListFiles(getDirectory());
    }

    /**
     * get list of key and values inside sharedPreferences
     * @return Map
     */
    public Map<String,String> getAllSharedPrefData(){
        Map<String,?> keys = getPreferences().getAll();
        Map<String,String> data = new HashMap<>();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            try {
                data.put(entry.getKey(), entry.getValue().toString());
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        return data;
    }


    /**
     * get SharedPreferences
     * @return SharedPreferences
     */
    public SharedPreferences getPreferences(){
        return sharedPreferences;
    }


    /**
     * check whether value is existed or not
     * @param key - key string
     * @return - value
     */
    public boolean contains(@NonNull String key){
        return sharedPreferences.contains(hashKey(key));
    }


    /* Save Data */

    public void putString(@NonNull String key, String value) {
        editor.putString(hashKey(key), hideValue(value)).apply();
    }

    public void putString(@NonNull String key, @StringRes int value) {
        editor.putString(hashKey(key), hideValue(mContext.getResources().getString(value))).apply();
    }

    public void putInt(@NonNull String key, int value) {
        editor.putString(hashKey(key), hideValue(Integer.toString(value))).apply();
    }

    public void putLong(@NonNull String key, long value) {
        editor.putString(hashKey(key), hideValue(Long.toString(value))).apply();
    }

    public void putDouble(@NonNull String key, double value) {
        editor.putString(hashKey(key), hideValue(Double.toString(value))).apply();
    }

    public void putFloat(@NonNull String key, float value) {
        editor.putString(hashKey(key), hideValue(Float.toString(value))).apply();
    }

    public void putBoolean(@NonNull String key, boolean value) {
        editor.putString(hashKey(key), hideValue(Boolean.toString(value))).apply();
    }

    public void putListString(@NonNull String key, List<String> value){
        editor.putString(hashKey(key), hideValue(value.toString())).apply();
    }

    public void putListFloat(@NonNull String key, List<Float> value){
        editor.putString(hashKey(key),hideValue(value.toString())).apply();
    }

    public void putListInteger(@NonNull String key, List<Integer> value){
        editor.putString(hashKey(key),hideValue(value.toString())).apply();
    }

    public void putListDouble(@NonNull String key, List<Double> value){
        editor.putString(hashKey(key),hideValue(value.toString())).apply();
    }

    public void putListLong(@NonNull String key, List<Long> value){
        editor.putString(hashKey(key),hideValue(value.toString())).apply();
    }

    public void putListBoolean(@NonNull String key, List<Boolean> value){
        editor.putString(hashKey(key),hideValue(value.toString())).apply();
    }

    public void putMap(@NonNull String key,Map<String,String> values){
        editor.putString(hashKey(key),hideValue(ConverterListUtils.convertMapToString(values))).apply();
    }

    public void putByte(@NonNull String key,byte[] bytes){
        editor.putString(hashKey(key),hideValue(new String(bytes))).apply();
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public String putImage(@NonNull String key, Bitmap bitmap){
        File imageFile = new File(getImageDirectory(mFolderName),"images_"+System.currentTimeMillis()+".png");
        if(FileUtils.saveBitmap(imageFile, bitmap)){
            concealCrypto.obscureFile(imageFile,true);
            editor.putString(hashKey(key),hideValue(imageFile.getAbsolutePath())).apply();
            return imageFile.getAbsolutePath();
        }
        return null;
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public String putImage(@NonNull String key, @DrawableRes int resId){
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
        if (bitmap!=null) {
            File imageFile = new File(getImageDirectory(mFolderName), "images_" + System.currentTimeMillis() + ".png");
            if (FileUtils.saveBitmap(imageFile, bitmap)) {
                concealCrypto.obscureFile(imageFile, true);
                editor.putString(hashKey(key), hideValue(imageFile.getAbsolutePath())).apply();
                return imageFile.getAbsolutePath();
            }
        }
        else{
            throw new IllegalArgumentException(resId+" : Drawable not found!");
        }

        return null;
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public String putImage(@NonNull String key, @Nullable File file){
        if (FileUtils.isFileForImage(file)) {
            File imageFile = FileUtils.moveFile(file,getImageDirectory(mFolderName));
            if (imageFile!=null && imageFile.exists()) {
                concealCrypto.obscureFile(imageFile,true);
                editor.putString(hashKey(key), hideValue(imageFile.getAbsolutePath())).apply();
                return imageFile.getAbsolutePath();
            }
        }
        return null;
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public File putFile(@NonNull String key,@Nullable File file,boolean deleteOldFile){

        if (file == null)
            return null;

        try {
            if (file.exists() && !FileUtils.isFileForImage(file)) {
                File enc = concealCrypto.obscureFile(file,deleteOldFile);
                editor.putString(hashKey(key), hideValue(enc.getAbsolutePath())).apply();
                return enc;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    /************************************
     * FETCHING DATA FROM SHAREDPREFS
     ************************************/
    public String getString(@NonNull String key){
        return concealCrypto.deObscure(sharedPreferences.getString(hashKey(key),null));
    }

    public String getString(@NonNull String key,String defaultValue){
        return concealCrypto.deObscure(sharedPreferences.getString(hashKey(key),defaultValue));
    }

    public Integer getInt(@NonNull String key){
        try {
            String value = getString(key);
            if (value == null)
                return -99;

            return Integer.parseInt(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Integer data type",e);
            return -99;
        }
    }

    public Integer getInt(@NonNull String key,int defaultValue){
        try {
            String value = getString(key);

            if (value == null)
                return defaultValue;

            return Integer.parseInt(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Integer data type",e);
            return -99;
        }
    }

    public Float getFloat(@NonNull String key){
        try {
            String value = getString(key);
            if (value == null)
                return 0f;

            return Float.parseFloat(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Float data type",e);
            return 0f;
        }
    }

    public Float getFloat(@NonNull String key,float defaultValue){
        try {
            String value = getString(key);

            if (value == null)
                return defaultValue;

            return Float.parseFloat(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Float data type",e);
            return defaultValue;
        }
    }

    public Double getDouble(@NonNull String key){
        try {
            String value = getString(key);
            if (value == null)
                return 0D;

            return Double.parseDouble(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Double data type",e);
            return 0D;
        }
    }

    public Double getDouble(@NonNull String key,double defaultValue){
        try {
            String value = getString(key);

            if (value == null)
                return defaultValue;

            return Double.parseDouble(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Double data type",e);
            return defaultValue;
        }
    }


    public Long getLong(@NonNull String key){
        try {
            String value = getString(key);
            if (value == null)
                return 0L;

            return Long.parseLong(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Long data type",e);
            return 0L;
        }
    }

    public Long getLong(@NonNull String key,long defaultValue){
        try {
            String value = getString(key);

            if (value == null)
                return defaultValue;

            return Long.parseLong(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Long data type",e);
            return defaultValue;
        }
    }

    public Boolean getBoolean(@NonNull String key){
        try {
            String value = getString(key);
            return value != null && Boolean.parseBoolean(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Boolean data type",e);
            return false;
        }
    }

    public Boolean getBoolean(@NonNull String key,boolean defaultValue){
        try {
            String value = getString(key);
            if (value == null)
                return defaultValue;

            return Boolean.parseBoolean(value);
        }
        catch (Exception e){
            throwRunTimeException("Unable to convert to Boolean data type",e);
            return false;
        }
    }

    public List<String> getListString(@NonNull String key){
        return ConverterListUtils.toStringArray(getString(key));
    }

    public List<Float> getListFloat(@NonNull String key){
        return ConverterListUtils.toFloatArray(getString(key));
    }

    public List<Double> getListDouble(@NonNull String key){
        return ConverterListUtils.toDoubleArray(getString(key));
    }

    public List<Boolean> getListBoolean(@NonNull String key){
        return ConverterListUtils.toBooleanArray(getString(key));
    }

    public List<Integer> getListInteger(@NonNull String key){
        return ConverterListUtils.toIntArray(getString(key));
    }

    public List<Long> getListLong(@NonNull String key){
        return ConverterListUtils.toLongArray(getString(key));
    }

    public LinkedHashMap<String,String> getMaps(@NonNull String key){
        return ConverterListUtils.convertStringToMap(getString(key));
    }

    public byte[] getArrayBytes(@NonNull String key){
        return getString(key).getBytes();
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public Bitmap getImage(@NonNull String key){
        String path = getString(key);
        if (path !=null) {
            try {
                File file = new File(path);
                return BitmapFactory.decodeFile(concealCrypto.deObscureFile(file,true).getAbsolutePath());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public File getFile(@NonNull String key,boolean deleteOldFile){
        try {
            String path = getString(key);
            if (path ==null) return null;

            File getFile = new File(path);
            if (getFile.exists()) {
                File dec = concealCrypto.deObscureFile(getFile,deleteOldFile);
                if (dec == null)
                    throw new Exception("File can't decrypt");

                return dec;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static class UserPref{
        private static String NAME = "user.username";
        private static String FULLNAME = "user.fullname";
        private static String FIRST_NAME = "user.first_name";
        private static String LAST_NAME = "user.last_name";
        private static String AGE = "user.age";
        private static String GENDER = "user.gender";
        private static String BIRTH_DATE = "user.dob";
        private static String ADDRESS = "user.address";
        private static String EMAIL = "user.email";
        private static String PUSH_TOKEN = "user.push.token";
        private static String PHONE_NO = "user.phone_number";
        private static String MOBILE_NO = "user.mobile_number";
        private static String DEVICE_ID = "user.device.id";
        private static String HAS_LOGIN = "user.has_login";
        private static String PASSWORD = "user.password";
        private static String FIRST_TIME_USER = "user.first_time";

        public UserPref() {
            if (editor == null){
                throw new IllegalArgumentException("Need to initialize ConcealPrefRepository.PreferencesBuilder first");
            }
        }

        public UserPref setUserName(String name){
            editor.putString(hashKey(NAME),hideValue(name));
            return this;
        }
        public UserPref setFullName(String fullName){
            editor.putString(hashKey(FULLNAME),hideValue(fullName));
            return this;
        }
        public UserPref setFirstName(String firstName){
            editor.putString(hashKey(FIRST_NAME),hideValue(firstName));
            return this;
        }
        public UserPref setLastName(String lastName){
            editor.putString(hashKey(LAST_NAME),hideValue(lastName));
            return this;
        }
        public UserPref setAge(int age){
            editor.putString(hashKey(AGE),hideValue(String.valueOf(age)));
            return this;
        }
        public UserPref setGender(String gender){
            editor.putString(hashKey(GENDER),hideValue(gender));
            return this;
        }
        public UserPref setBirthDate(String birthDate){
            editor.putString(hashKey(BIRTH_DATE),hideValue(birthDate));
            return this;
        }
        public UserPref setAddress(String address){
            editor.putString(hashKey(ADDRESS),hideValue(address));
            return this;
        }
        public UserPref setEmail(String email){
            editor.putString(hashKey(EMAIL),hideValue(email));
            return this;
        }
        public UserPref setPushToken(String token){
            editor.putString(hashKey(PUSH_TOKEN),hideValue(token));
            return this;
        }
        public UserPref setPhoneNumber(String phoneNumber){
            editor.putString(hashKey(PHONE_NO),hideValue(phoneNumber));
            return this;
        }
        public UserPref setMobileNumber(String mobileNumber){
            editor.putString(hashKey(MOBILE_NO),hideValue(mobileNumber));
            return this;
        }
        public UserPref setDeviceId(String deviceId){
            editor.putString(hashKey(DEVICE_ID),hideValue(deviceId));
            return this;
        }
        public UserPref setLogin(boolean login){
            editor.putString(hashKey(HAS_LOGIN),hideValue(String.valueOf(login)));
            return this;
        }
        public UserPref setPassword(String password){
            editor.putString(hashKey(PASSWORD),hideValue(password));
            return this;
        }
        public UserPref setFirstTimeUser(boolean firstTime){
            editor.putString(hashKey(FIRST_TIME_USER),hideValue(String.valueOf(firstTime)));
            return this;
        }

        public String getUserName(){
            return returnValue(NAME);
        }
        public String getFullName(){
            return returnValue(FULLNAME);
        }
        public String getFirstName(){
            return returnValue(FIRST_NAME);
        }
        public String getLastName(){
            return returnValue(LAST_NAME);
        }
        public Integer getAge(){
            try {
                return Integer.parseInt(returnValue(AGE));
            }
            catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        public String getGender(){
            return returnValue(GENDER);
        }
        public String getBirthDate(){
            return returnValue(BIRTH_DATE);
        }
        public String getAddress(){
            return returnValue(ADDRESS);
        }
        public String getEmail(){
            return returnValue(EMAIL);
        }
        public String getPushToken(){
            return returnValue(PUSH_TOKEN);
        }
        public String getPhoneNumber(){
            return returnValue(PHONE_NO);
        }
        public String getMobileNumber(){
            return returnValue(MOBILE_NO);
        }
        public String getDeviceId(){
            return returnValue(DEVICE_ID);
        }
        public Boolean hasLogin(){
            try {
                return Boolean.parseBoolean(returnValue(HAS_LOGIN));
            }
            catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        public String getPassword(){
            return returnValue(PASSWORD);
        }

        public Boolean isFirstTimeUser(){
            try {
                return Boolean.parseBoolean(returnValue(FIRST_TIME_USER));
            }
            catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        private String returnValue(String KEY){
            return concealCrypto.deObscure(sharedPreferences.getString(hashKey(KEY),null));
        }

        public void apply() {
            editor.apply();
        }
    }


    /******************************************
     * SharedPreferences Editor Builder
     ******************************************/
    public static final class Editor {
        public Editor() {
            if (editor ==null){
                throw new IllegalArgumentException("Need to initialize ConcealPrefRepository.PreferencesBuilder first");
            }
        }

        public Editor putString(@NonNull String key, String value) {
            editor.putString(hashKey(key), hideValue(value));
            return this;
        }

        public Editor putInt(@NonNull String key, int value) {
            editor.putString(hashKey(key), hideValue(Integer.toString(value)));
            return this;
        }

        public Editor putLong(@NonNull String key, long value) {
            editor.putString(hashKey(key), hideValue(Long.toString(value)));
            return this;
        }

        public Editor putDouble(@NonNull String key, double value) {
            editor.putString(hashKey(key), hideValue(Double.toString(value)));
            return this;
        }

        public Editor putFloat(@NonNull String key, float value) {
            editor.putString(hashKey(key), hideValue(Float.toString(value)));
            return this;
        }

        public Editor putBoolean(@NonNull String key, boolean value) {
            editor.putString(hashKey(key), hideValue(Boolean.toString(value)));
            return this;
        }

        public Editor putListString(@NonNull String key, List<String> value){
            editor.putString(hashKey(key),hideValue(value.toString()));
            return this;
        }

        public Editor putListFloat(@NonNull String key, List<Float> value){
            editor.putString(hashKey(key),hideValue(value.toString()));
            return this;
        }

        public Editor putListInteger(@NonNull String key, List<Integer> value){
            editor.putString(hashKey(key),hideValue(value.toString()));
            return this;
        }

        public Editor putListDouble(@NonNull String key, List<Double> value){
            editor.putString(hashKey(key),hideValue(value.toString()));
            return this;
        }

        public Editor putListLong(@NonNull String key, List<Long> value){
            editor.putString(hashKey(key),hideValue(value.toString()));
            return this;
        }

        public Editor putListBoolean(@NonNull String key, List<Boolean> value){
            editor.putString(hashKey(key),hideValue(value.toString()));
            return this;
        }

        public Editor putByte(@NonNull String key,byte[] bytes){
            editor.putString(hashKey(key),hideValue(new String(bytes)));
            return this;
        }

        @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
        public Editor putImage(@NonNull String key, @DrawableRes int resId, Context context){
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            if (bitmap!=null) {
                File imageFile = new File(getImageDirectory(mFolderName), "images_" + System.currentTimeMillis() + ".png");
                if (FileUtils.saveBitmap(imageFile, bitmap)) {
                    editor.putString(hashKey(key), hideValue(concealCrypto.obscureFile(imageFile, true).getAbsolutePath()));
                }
            }
            else{
                throw new RuntimeException(resId+" : Drawable not found!");
            }
            return this;
        }

        @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
        public Editor putImage(@NonNull String key, Bitmap bitmap){
            File imageFile = new File(getImageDirectory(mFolderName),"images_"+System.currentTimeMillis()+".png");
            if(FileUtils.saveBitmap(imageFile, bitmap)){
                editor.putString(hashKey(key),hideValue(concealCrypto.obscureFile(imageFile,true).getAbsolutePath()));
            }
            return this;
        }

        @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
        public Editor putImage(@NonNull String key, File file){
            if (FileUtils.isFileForImage(file)) {
                File imageFile = FileUtils.moveFile(file,getImageDirectory(mFolderName));
                if (imageFile!=null && imageFile.exists()) {
                    concealCrypto.obscureFile(imageFile,true);
                    editor.putString(hashKey(key), hideValue(imageFile.getAbsolutePath()));
                }
            }
            return this;
        }

        @RequiresPermission(allOf = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
        public Editor putFile(@NonNull String key,File file,boolean deleteOldFile){
            try {
                if (file.exists() && !FileUtils.isFileForImage(file)) {
                    File enc = concealCrypto.obscureFile(file,deleteOldFile);
                    editor.putString(hashKey(key), hideValue(enc.getAbsolutePath()));
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            return this;
        }
        public Editor remove(@NonNull String key) {
            editor.remove(hashKey(key));
            return this;
        }

        public Editor putMap(@NonNull String key,Map<String,String> values){
            editor.putString(hashKey(key),hideValue(ConverterListUtils.convertMapToString(values)));
            return this;
        }

        public Editor clear() {
            editor.clear();
            return this;
        }

        public boolean commit() {
            return editor.commit();
        }


        public void apply() {
            editor.apply();
        }
    }


    /************************v***************************************************************
     * Preferences builder,  ConcealPrefRepository.PreferencesBuilder
     ****************************************************************************************/
    public static class PreferencesBuilder{

        private WeakReference<Context> mContext;
        private CryptoConfig mKeyChain = CryptoConfig.KEY_256;
        private String mPrefname = null;
        private String mFolderName = null;
        private boolean mEnabledCrypto = false;
        private boolean mEnableCryptKey = false;
        private String mEntityPasswordRaw = null;
        private SharedPreferences sharedPreferences;
        private OnDataChangeListener onDataChangeListener;

        public PreferencesBuilder(Context context) {
            mContext = new WeakReference<>(context.getApplicationContext());
        }

        public PreferencesBuilder useDefaultPrefStorage(){
            return this;
        }

        public PreferencesBuilder useThisPrefStorage(String prefName){
            mPrefname = prefName;
            return this;
        }

        /**
         * Enable encryption for keys-values
         * @param encryptKey true/false to enable encryption for key
         * @param encryptValue true/false to enable encryption for values
         * @return PreferencesBuilder
         */
        public PreferencesBuilder enableCrypto(boolean encryptKey,boolean encryptValue){
            mEnabledCrypto = encryptValue;
            mEnableCryptKey = encryptKey;
            return this;
        }

        /**
         * Use Conceal keychain
         * @param keyChain Cryptography type
         * @return PreferencesBuilder
         */
        public PreferencesBuilder sharedPrefsBackedKeyChain(CryptoConfig keyChain){
            mKeyChain = keyChain;
            return this;
        }

        /**
         * Setup password / paraphrase for encryption
         * @param password string password
         * @return PreferencesBuilder
         */
        public PreferencesBuilder createPassword(String password){
            mEntityPasswordRaw = password;
            return this;
        }

        /**
         * Set folder name to store files and images
         * @param folderName folder path
         * @return PreferencesBuilder
         */
        public PreferencesBuilder setFolderName(String folderName){
            mFolderName = folderName;
            return this;
        }

        /**
         * Listen to data changes
         * @param listener OnDataChangeListener listener
         * @return PreferencesBuilder
         */
        public PreferencesBuilder setPrefListener(OnDataChangeListener listener){
            onDataChangeListener = listener;
            return this;
        }

        /**
         * Create Preferences
         * @return ConcealPrefRepository
         */
        public ConcealPrefRepository create(){

            if (this.mContext == null){
                throw new RuntimeException("Context cannot be null");
            }

            if(mFolderName !=null){
                File file = new File(mFolderName);
                try {
                    file.getCanonicalPath();
                    mFolderName = (mFolderName.startsWith("."))? mFolderName.substring(1):mFolderName;
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Folder Name is not Valid",e);
                }
            }
            else{
                mFolderName = DEFAULT_MAIN_FOLDER;
            }

            if (mPrefname!=null){
                sharedPreferences = this.mContext.get().getSharedPreferences(CipherUtils.obscureEncodeSixFourString(mPrefname.getBytes()), MODE_PRIVATE);
            }
            else {
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext.get());
            }

            return new ConcealPrefRepository(this);

        }
    }

    // ============================================================================================================

    private static void throwRunTimeException(String message, Throwable throwable){
        new RuntimeException(message,throwable).printStackTrace();
    }

    private static String hashKey(String key){
        if (key == null || key.equals(""))
            throw new NullPointerException("Key cannot be null or empty");

        return concealCrypto.hashKey(key);
    }

    private static String hideValue(String value){
        return concealCrypto.obscure(value);
    }

    // ============================================================================================================

    /***
     * get List of encrypted file
     * @param parentDir - root directory
     * @return File
     */
    private List<CryptoFile> getListFiles(@Nullable File parentDir) {
        List<CryptoFile> inFiles = new ArrayList<>();
        try {
            if (parentDir!=null) {
                File[] files = parentDir.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        inFiles.addAll(getListFiles(file));
                    } else {
                        if (file.getName().startsWith(DEFAULT_PREFIX_FILENAME)) {
                            CryptoFile cryptoFile = new CryptoFile();
                            cryptoFile.setFileName(file.getName());
                            cryptoFile.setPath(file.getAbsolutePath());
                            cryptoFile.setType(file.getParent());
                            inFiles.add(cryptoFile);
                        }
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return inFiles;
    }


    /***
     * get default directory
     * @return File
     */
    @Nullable
    private static File getDirectory(){
        File file = new File(DEFAULT_DIRECTORY+mFolderName+"/"+DEFAULT_IMAGE_FOLDER);
        if (file.exists())
            return file;

        return null;
    }

    /***
     * get default folder
     * @return File
     */
    @Nullable
    private static File getImageDirectory(String mFolderName){
        File file = new File(DEFAULT_DIRECTORY+mFolderName+"/"+DEFAULT_IMAGE_FOLDER);
        if (file.mkdirs())
            return file;
        if (file.exists())
            return file;

        return null;
    }

}
