/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.dialogs;

import static android.util.Base64.URL_SAFE;
import static com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.AT;
import static com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.COLON;
import static com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.SLASH;
import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;
import static com.amaze.filemanager.utils.smb.SmbUtil.PARAM_DISABLE_IPC_SIGNING_CHECK;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.databinding.SmbDialogBinding;
import com.amaze.filemanager.filesystem.smb.CifsContexts;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.utils.PasswordUtil;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.smb.SmbUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.DialogFragment;

import jcifs.smb.SmbFile;
import kotlin.text.Charsets;

/** SMB connection editor with a simple default flow and optional advanced settings. */
public class SmbConnectDialog extends DialogFragment {

  public static final String TAG = "smbdialog";
  public static final String ARG_NAME = "name";
  public static final String ARG_PATH = "path";
  public static final String ARG_EDIT = "edit";

  private static final Logger LOG = LoggerFactory.getLogger(SmbConnectDialog.class);

  private SmbConnectionListener smbConnectionListener;
  private SmbDialogBinding binding;
  private String emptyAddress;
  private String invalidDomain;
  private String invalidUsername;

  public interface SmbConnectionListener {
    void addConnection(
        boolean edit,
        @NonNull String name,
        @NonNull String encryptedPath,
        @Nullable String oldname,
        @Nullable String oldPath);

    void deleteConnection(String name, String path);
  }

  @VisibleForTesting
  public void setSmbConnectionListener(SmbConnectionListener smbConnectionListener) {
    this.smbConnectionListener = smbConnectionListener;
  }

  @VisibleForTesting
  public SmbConnectionListener getSmbConnectionListener() {
    return smbConnectionListener;
  }

  @VisibleForTesting
  public SmbDialogBinding getBinding() {
    return binding;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle arguments = requireArguments();
    final boolean edit = arguments.getBoolean(ARG_EDIT, false);
    final String path = arguments.getString(ARG_PATH, "");
    final String name = arguments.getString(ARG_NAME, "");
    final Context context = requireActivity();

    emptyAddress = getString(R.string.cant_be_empty, getString(R.string.server_address));
    invalidDomain = getString(R.string.invalid, getString(R.string.domain));
    invalidUsername = getString(R.string.invalid, getString(R.string.username).toLowerCase());

    if (requireActivity() instanceof SmbConnectionListener && smbConnectionListener == null) {
      smbConnectionListener = (SmbConnectionListener) requireActivity();
    }

    binding = SmbDialogBinding.inflate(LayoutInflater.from(context));
    setupFields(edit, name, path);

    MaterialAlertDialogBuilder builder =
        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.smb_connection)
            .setView(binding.getRoot())
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss())
            .setPositiveButton(edit ? R.string.smb_save : R.string.smb_connect, null);

    if (edit) {
      builder.setNeutralButton(R.string.delete, null);
    }

    AlertDialog dialog = builder.create();
    dialog.setOnShowListener(
        ignored -> {
          final int accentColor = ((MainActivity) requireActivity()).getAccent();
          final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
          final Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
          positiveButton.setTextColor(accentColor);
          negativeButton.setTextColor(accentColor);
          positiveButton.setOnClickListener(v -> validateAndSave(dialog, edit, name, path));
          if (edit) {
            final Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setTextColor(accentColor);
            neutralButton.setOnClickListener(
                v -> {
                  if (smbConnectionListener != null) {
                    smbConnectionListener.deleteConnection(name, path);
                  }
                  dismiss();
                });
          }
        });
    return dialog;
  }

  private void setupFields(boolean edit, String name, String path) {
    final TextInputLayout ipTIL = binding.ipTIL;
    final TextInputLayout domainTIL = binding.domainTIL;
    final TextInputLayout usernameTIL = binding.usernameTIL;
    final AppCompatEditText conName = binding.connectionET;
    final AppCompatEditText ip = binding.ipET;
    final AppCompatEditText share = binding.shareET;
    final AppCompatEditText domain = binding.domainET;
    final AppCompatEditText user = binding.usernameET;
    final AppCompatEditText pass = binding.passwordET;
    final MaterialCheckBox anonymous = binding.chkSmbAnonymous;
    final MaterialCheckBox disableIpcSignature = binding.chkSmbDisableIpcSignature;
    final AppCompatTextView help = binding.wanthelp;
    final MaterialButton advancedToggle = binding.advancedToggle;

    // Material's outlined field fallback can resolve to a much darker secondary palette than the
    // rest of Amaze's Monet controls. Pin the focused stroke/button role to Amaze's resolved
    // accent so network dialogs use the same readable tonal accent as switches and actions.
    final int accentColor = ((MainActivity) requireActivity()).getAccent();
    final TextInputLayout[] accentedInputs = {
      binding.ipTIL,
      binding.usernameTIL,
      binding.passwordTIL,
      binding.connectionTIL,
      binding.shareTIL,
      binding.domainTIL
    };
    for (TextInputLayout inputLayout : accentedInputs) {
      inputLayout.setBoxStrokeColor(accentColor);
      // TextInputLayout otherwise keeps its focused floating label on Material colorPrimary,
      // which is deliberately darker for Amaze's large app-bar surface. Use the control accent
      // for the focused label as well so outline and label are one consistent readable tone.
      inputLayout.setHintTextColor(ColorStateList.valueOf(accentColor));
    }
    tintTextCursor(ip, accentColor);
    tintTextCursor(user, accentColor);
    tintTextCursor(pass, accentColor);
    tintTextCursor(conName, accentColor);
    tintTextCursor(share, accentColor);
    tintTextCursor(domain, accentColor);
    advancedToggle.setTextColor(accentColor);

    ip.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            ipTIL.setError(s.length() == 0 ? emptyAddress : null);
          }
        });
    domain.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            domainTIL.setError(s.toString().contains(";") ? invalidDomain : null);
          }
        });
    user.addTextChangedListener(
        new SimpleTextWatcher() {
          @Override
          public void afterTextChanged(@NonNull Editable s) {
            usernameTIL.setError(s.toString().contains(String.valueOf(COLON)) ? invalidUsername : null);
          }
        });

    anonymous.setOnCheckedChangeListener(
        (buttonView, checked) -> {
          user.setEnabled(!checked);
          pass.setEnabled(!checked);
        });

    help.setOnClickListener(
        v -> GeneralDialogCreation.showSMBHelpDialog(requireActivity(), ((MainActivity) requireActivity()).getAccent()));

    advancedToggle.setOnClickListener(
        v -> {
          boolean show = binding.advancedContainer.getVisibility() != View.VISIBLE;
          binding.advancedContainer.setVisibility(show ? View.VISIBLE : View.GONE);
          advancedToggle.setText(show ? R.string.smb_hide_advanced_options : R.string.smb_advanced_options);
        });

    if (edit) {
      populateExistingConnection(name, path, conName, ip, share, domain, user, pass, anonymous, disableIpcSignature);
      binding.advancedContainer.setVisibility(View.VISIBLE);
      advancedToggle.setText(R.string.smb_hide_advanced_options);
    } else if (!TextUtils.isEmpty(path)) {
      SmbTarget target = parseTarget(path, "");
      conName.setText(TextUtils.isEmpty(name) ? target.server : name);
      ip.setText(target.server);
      share.setText(target.share);
      user.requestFocus();
    } else {
      ip.requestFocus();
    }
  }

  private static void tintTextCursor(AppCompatEditText editText, int color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && editText.getTextCursorDrawable() != null) {
      editText.getTextCursorDrawable().setTint(color);
    }
  }

  private void populateExistingConnection(
      String name,
      String path,
      AppCompatEditText conName,
      AppCompatEditText ip,
      AppCompatEditText share,
      AppCompatEditText domain,
      AppCompatEditText user,
      AppCompatEditText pass,
      MaterialCheckBox anonymous,
      MaterialCheckBox disableIpcSignature) {
    conName.setText(name);
    try {
      Uri uri = Uri.parse(path);
      String userInfo = uri.getUserInfo();
      if (userInfo != null) {
        String info = decode(userInfo, Charsets.UTF_8.name());
        int domainDelimiter = info.contains(";") ? info.indexOf(';') : -1;
        if (domainDelimiter >= 0) {
          domain.setText(info.substring(0, domainDelimiter));
          info = info.substring(domainDelimiter + 1);
        }
        if (info.contains(":")) {
          user.setText(info.substring(0, info.indexOf(COLON)));
          try {
            String decrypted =
                PasswordUtil.INSTANCE.decryptPassword(
                    requireContext(), info.substring(info.indexOf(COLON) + 1), URL_SAFE);
            pass.setText(decode(decrypted, Charsets.UTF_8.name()));
          } catch (GeneralSecurityException | IOException e) {
            LOG.warn("Error decrypting SMB password", e);
          }
        } else {
          user.setText(info);
        }
      } else {
        anonymous.setChecked(true);
      }
      ip.setText(uri.getHost());
      String existingShare = uri.getPath();
      if (existingShare != null) {
        share.setText(existingShare.replaceFirst("^/", "").replaceAll("/$", ""));
      }

      UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(path);
      if (sanitizer.hasParameter(PARAM_DISABLE_IPC_SIGNING_CHECK)) {
        disableIpcSignature.setChecked(
            Boolean.parseBoolean(sanitizer.getValue(PARAM_DISABLE_IPC_SIGNING_CHECK)));
      }
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      LOG.warn("Failed to load SMB connection into editor", e);
    }
  }

  private void validateAndSave(
      AlertDialog dialog, boolean edit, @Nullable String oldName, @Nullable String oldPath) {
    String rawServer = text(binding.ipET);
    String rawShare = text(binding.shareET);
    SmbTarget target = parseTarget(rawServer, rawShare);
    String domain = text(binding.domainET);
    String username = text(binding.usernameET);
    String password = text(binding.passwordET);

    TextInputLayout firstInvalid = null;
    if (TextUtils.isEmpty(target.server)) {
      binding.ipTIL.setError(emptyAddress);
      firstInvalid = binding.ipTIL;
    } else {
      binding.ipTIL.setError(null);
    }
    if (domain.contains(";")) {
      binding.domainTIL.setError(invalidDomain);
      if (firstInvalid == null) firstInvalid = binding.domainTIL;
    }
    if (username.contains(":")) {
      binding.usernameTIL.setError(invalidUsername);
      if (firstInvalid == null) firstInvalid = binding.usernameTIL;
    }
    if (firstInvalid != null) {
      firstInvalid.requestFocus();
      return;
    }

    // Reflect normalized UNC/smb:// input in the editor so the user can see what will be saved.
    binding.ipET.setText(target.server);
    binding.shareET.setText(target.share);

    boolean anonymous =
        binding.chkSmbAnonymous.isChecked()
            || (TextUtils.isEmpty(username) && TextUtils.isEmpty(password));
    SmbFile smbFile =
        createSMBPath(
            new String[] {target.server, username, password, domain, target.share},
            anonymous,
            binding.chkSmbDisableIpcSignature.isChecked());
    if (smbFile == null) {
      Toast.makeText(requireContext(), R.string.smb_error_invalid_address, Toast.LENGTH_LONG).show();
      return;
    }

    String connectionName = text(binding.connectionET);
    if (TextUtils.isEmpty(connectionName)) {
      connectionName = target.server;
    }

    try {
      String encryptedPath = SmbUtil.getSmbEncryptedPath(requireActivity(), smbFile.getPath());
      if (binding.chkSmbDisableIpcSignature.isChecked()) {
        encryptedPath += "?" + PARAM_DISABLE_IPC_SIGNING_CHECK + "=true";
      }
      if (smbConnectionListener != null) {
        smbConnectionListener.addConnection(edit, connectionName, encryptedPath, oldName, oldPath);
      }
      dialog.dismiss();
    } catch (Exception e) {
      LOG.warn("Failed to save SMB connection", e);
      Toast.makeText(requireContext(), R.string.smb_error_generic, Toast.LENGTH_LONG).show();
    }
  }

  private static String text(AppCompatEditText view) {
    return view.getText() == null ? "" : view.getText().toString().trim();
  }

  /** Accept a host/IP, smb:// URL, or Windows UNC path and normalize it for jcifs. */
  @VisibleForTesting
  static SmbTarget parseTarget(String serverValue, String shareValue) {
    String server = serverValue == null ? "" : serverValue.trim();
    String share = shareValue == null ? "" : shareValue.trim();

    String candidate = server;
    if (share.startsWith("\\\\") || share.startsWith("//") || share.startsWith("smb://")) {
      candidate = share;
      share = "";
    }

    candidate = candidate.replace('\\', '/');
    if (candidate.regionMatches(true, 0, SMB_URI_PREFIX, 0, SMB_URI_PREFIX.length())) {
      candidate = candidate.substring(SMB_URI_PREFIX.length());
    }
    while (candidate.startsWith("/")) candidate = candidate.substring(1);

    int slash = candidate.indexOf('/');
    if (slash >= 0) {
      server = candidate.substring(0, slash).trim();
      if (TextUtils.isEmpty(share)) share = candidate.substring(slash + 1);
    } else {
      server = candidate.trim();
    }

    share = share.replace('\\', '/').trim();
    while (share.startsWith("/")) share = share.substring(1);
    while (share.endsWith("/")) share = share.substring(0, share.length() - 1);
    return new SmbTarget(server, share);
  }

  @VisibleForTesting
  static final class SmbTarget {
    final String server;
    final String share;

    SmbTarget(String server, String share) {
      this.server = server;
      this.share = share;
    }
  }

  // Build a URL with encoded credentials and path segments. The returned SmbFile is used as a
  // normalized path container; real listing later goes through SmbUtil.create(), which supplies
  // the jcifs authentication context.
  private SmbFile createSMBPath(String[] auth, boolean anonymous, boolean disableIpcSignCheck) {
    try {
      String server = auth[0];
      String domain = auth[3];
      String share = auth[4];

      StringBuilder sb = new StringBuilder(SMB_URI_PREFIX);
      if (!TextUtils.isEmpty(domain)) sb.append(encodeComponent(domain + ";"));
      if (!anonymous) {
        sb.append(encodeComponent(auth[1]))
            .append(COLON)
            .append(encodeComponent(auth[2]))
            .append(AT);
      }
      sb.append(server).append(SLASH);
      if (!TextUtils.isEmpty(share)) {
        sb.append(encodeSharePath(share)).append(SLASH);
      }
      return new SmbFile(
          sb.toString(),
          CifsContexts.createWithDisableIpcSigningCheck(sb.toString(), disableIpcSignCheck));
    } catch (MalformedURLException | UnsupportedEncodingException | IllegalArgumentException e) {
      LOG.warn("Failed to build SMB path", e);
      return null;
    }
  }

  private static String encodeComponent(String value) throws UnsupportedEncodingException {
    return encode(value, Charsets.UTF_8.name()).replace("+", "%20");
  }

  private static String encodeSharePath(String share) throws UnsupportedEncodingException {
    StringBuilder encoded = new StringBuilder();
    for (String segment : share.split("/")) {
      if (segment.isEmpty()) continue;
      if (encoded.length() > 0) encoded.append('/');
      encoded.append(encodeComponent(segment));
    }
    return encoded.toString();
  }
}
