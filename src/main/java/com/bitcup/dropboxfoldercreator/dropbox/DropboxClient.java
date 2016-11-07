package com.bitcup.dropboxfoldercreator.dropbox;

import com.bitcup.dropboxfoldercreator.config.Config;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.FolderSharingInfo;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.sharing.*;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author bitcup
 */
public class DropboxClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropboxClient.class);
    private static final DropboxClient instance = new DropboxClient();

    private final DbxClientV2 dbxClient;

    public static void main(String[] args) throws Exception {
        DropboxClient client = DropboxClient.getInstance();
        // create top level
        client.createFolderIfNotExists(Config.val("dropbox.top.folder"), "");
        // create sub folders and share
        FolderMetadata student1 = client.createFolderIfNotExists(Config.val("test.student1.folder"), Config.val("dropbox.top.folder"));
        client.shareFolderAndGetUrl(student1, Config.val("test.student1.email"), AccessLevel.EDITOR);
        FolderMetadata student2 = client.createFolderIfNotExists(Config.val("test.student2.folder"), Config.val("dropbox.top.folder"));
        client.shareFolderAndGetUrl(student2, Config.val("test.student2.email"), AccessLevel.EDITOR);
    }

    private DropboxClient() {
        DbxRequestConfig config = new DbxRequestConfig("lynchYearbookConfig");
        dbxClient = new DbxClientV2(config, Config.val("dropbox.access.token"));
    }

    public static DropboxClient getInstance() {
        return instance;
    }

    public FolderMetadata createFolderIfNotExists(String folderName, String path) throws DbxException {
        String fullPath = path + folderName;
        Optional<Metadata> matching = dbxClient.files().listFolder(path).getEntries().stream().filter(metadata -> metadata.getPathLower().equals(fullPath.toLowerCase())).findFirst();
        LOGGER.info("creating folder: {}...", fullPath);
        FolderMetadata folder;
        if (matching.isPresent()) {
            folder = (FolderMetadata) matching.get();
            LOGGER.info("folder: {} already exists - skip creation", fullPath);
        } else {
            folder = dbxClient.files().createFolder(fullPath);
            LOGGER.info("folder: {} created", fullPath);
        }
        return folder;
    }

    public String shareFolderAndGetUrl(FolderMetadata folder, String email, AccessLevel accessLevel) throws DbxException {
        LOGGER.info("sharing folder: {} with user: {}...", folder.getPathDisplay(), email);
        final FolderSharingInfo sharingInfo = folder.getSharingInfo();
        String url;
        if (sharingInfo != null) {
            String inviteeEmail = dbxClient.sharing().listFolderMembers(sharingInfo.getSharedFolderId()).getInvitees().get(0).getInvitee().getEmailValue();
            if (email.equals(inviteeEmail)) {
                url = dbxClient.sharing().getFolderMetadata(sharingInfo.getSharedFolderId()).getPreviewUrl();
                LOGGER.info("folder: {} already shared with user: {} via url: {} - skip sharing", folder.getPathDisplay(), email, url);
            } else {
                LOGGER.info("folder: {} already shared, but not with user: {} - adding", folder.getPathDisplay(), email);
                url = addFolderMember(folder, email, accessLevel, sharingInfo.getSharedFolderId());
            }
        } else {
            ShareFolderLaunch shareFolderLaunch = dbxClient.sharing().shareFolder(folder.getPathDisplay());
            SharedFolderMetadata sharedFolderMetadata = shareFolderLaunch.getCompleteValue();
            if (sharedFolderMetadata.getSharedFolderId() != null) {
                url = addFolderMember(folder, email, accessLevel, sharedFolderMetadata.getSharedFolderId());
            } else {
                throw new IllegalStateException("could not get initial sharing for folder: " + folder.getPathDisplay());
            }
        }
        return url;
    }

    private String addFolderMember(FolderMetadata folder, String email, AccessLevel accessLevel, String shareFolderId) throws DbxException {
        AddMember addMember = new AddMember(MemberSelector.email(email), accessLevel);
        dbxClient.sharing().addFolderMember(shareFolderId, Lists.newArrayList(addMember));
        String previewUrl = dbxClient.sharing().getFolderMetadata(shareFolderId).getPreviewUrl();
        LOGGER.info("folder: {} shared with user: {} via url: {}", folder.getPathDisplay(), email, previewUrl);
        return previewUrl;
    }
}
