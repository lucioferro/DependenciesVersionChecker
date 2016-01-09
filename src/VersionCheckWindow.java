import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import groovy.util.Node;
import groovy.util.XmlParser;
import groovy.xml.QName;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by taku on 2016/01/09.
 */
public class VersionCheckWindow implements ToolWindowFactory {

    private JPanel toowWindowContent;
    private JButton versionCheckButton;
    private JTextArea inputArea;
    private JEditorPane editorPane1;

    public VersionCheckWindow() {
        versionCheckButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Notifications.Bus.notify(new Notification("versionCheckStart", "Dependencies Version Checker", "Version check started.", NotificationType.INFORMATION));

                String gradleScript = inputArea.getText();
                final List<String> libraryDeclarationTexts = GradleScriptParsingUtils.extractLibraryDeclarationTexts(gradleScript);
                final List<String> metaDataUrls = GradleScriptParsingUtils.createMetaDataUrls(libraryDeclarationTexts);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> latestVersions = getLatestVersions(metaDataUrls);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("<table>")
                                .append("<tr><th align=\"left\">Library</th><th align=\"left\">Latest version</th></tr>");
                        for (int i = 0; i < libraryDeclarationTexts.size(); i++) {
                            stringBuilder
                                    .append("<tr>")
                                    .append("<td><a href=\"" + metaDataUrls.get(i) + "\">" + libraryDeclarationTexts.get(i) + "</a></td><td>" + latestVersions.get(i) + "</td>")
                                    .append("</tr>");
                        }
                        stringBuilder.append("</table>");
                        editorPane1.setText(stringBuilder.toString());

                        Notifications.Bus.notify(new Notification("versionCheckStart", "Dependencies Version Checker", "Version check finished.", NotificationType.INFORMATION));
                    }
                }).start();
            }
        });

        editorPane1.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(toowWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private List<String> getLatestVersions(List<String> metaDataUrls) {
        List<String> latestVersions = new ArrayList<String>();

        for (String metaDataUrl : metaDataUrls) {
            try {
                XmlParser xmlParser = new XmlParser();
                Node node = xmlParser.parse(metaDataUrl);
                String latestVersion = node.getAt(QName.valueOf("versioning")).getAt("latest").text();
                latestVersions.add(latestVersion);
            } catch (Exception e1) {
                latestVersions.add("Not Found");
            }
        }
        return latestVersions;
    }
}