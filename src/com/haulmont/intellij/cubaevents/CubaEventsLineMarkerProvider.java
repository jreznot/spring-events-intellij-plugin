package com.haulmont.intellij.cubaevents;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import static com.haulmont.intellij.cubaevents.EventsDeclarations.EVENTS_CLASSNAME;
import static com.haulmont.intellij.cubaevents.EventsDeclarations.EVENTS_PUBLISH_METHODNAME;

/**
 * <p>
 * 1.fix package name for EventBus
 * <p>
 * 2. try use `GlobalSearchScope.projectScope(project)` to just search for project, but get NullPointerException,
 * the old use `GlobalSearchScope.allScope(project)` , it will search in project and libs,so slow
 */
public class CubaEventsLineMarkerProvider implements LineMarkerProvider {

    // todo support ApplicationListener<ApplicationEvent>
    // todo support @EventListener(AppContextInitializedEvent.class)

    public static final Icon EVENT_ICON = IconLoader.getIcon("/icons/event-icon.png");
    public static final Icon SENDER_ICON = IconLoader.getIcon("/icons/sender-icon.png");
    public static final Icon RECEIVER_ICON = IconLoader.getIcon("/icons/receiver-icon.png");

    public static final int MAX_USAGES = 100;

    private static void showEventUsages(MouseEvent e, PsiElement psiElement) {
        if (psiElement instanceof PsiIdentifier
                && psiElement.getParent() instanceof PsiClass) {

            Project project = psiElement.getParent().getProject();
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            PsiClass eventBusClass = javaPsiFacade.findClass(EVENTS_CLASSNAME, GlobalSearchScope.allScope(project));

            if (eventBusClass == null) {
                return;
            }
            PsiElement psiClass = psiElement.getParent();

            new ShowUsagesAction(new EventClassFilter())
                    .startFindUsages(psiClass, new RelativePoint(e), PsiUtilBase.findEditor(psiClass), MAX_USAGES);
        }
    }

    private static void showEventSenders(MouseEvent e, PsiElement psiElement) {
        if (psiElement instanceof PsiIdentifier
                && psiElement.getParent() instanceof PsiMethod) {

            Project project = psiElement.getParent().getProject();
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            PsiClass eventBusClass = javaPsiFacade.findClass(EVENTS_CLASSNAME, GlobalSearchScope.allScope(project));

            if (eventBusClass == null) {
                return;
            }

            PsiMethod postMethod = eventBusClass.findMethodsByName(EVENTS_PUBLISH_METHODNAME, false)[0];
            PsiMethod method = (PsiMethod) psiElement.getParent();
            if (method.getParameterList().isEmpty()) {
                return;
            }

            PsiTypeElement typeElement = method.getParameterList().getParameters()[0].getTypeElement();
            if (typeElement == null) {
                return;
            }

            PsiClass eventClass = ((PsiClassType) typeElement.getType()).resolve();

            new ShowUsagesAction(new SenderFilter(eventClass))
                    .startFindUsages(postMethod, new RelativePoint(e), PsiUtilBase.findEditor(psiElement.getParent()), MAX_USAGES);
        }
    }

    private static void showEventReceivers(MouseEvent e, PsiElement psiElement) {
        if (psiElement instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression expression = (PsiMethodCallExpression) psiElement;
            PsiType[] expressionTypes = expression.getArgumentList().getExpressionTypes();
            if (expressionTypes.length > 0) {
                PsiClass eventClass = PsiUtils.getClass(expressionTypes[0]);
                if (eventClass != null) {
                    new ShowUsagesAction(new ReceiverFilter())
                            .startFindUsages(eventClass, new RelativePoint(e), PsiUtilBase.findEditor(psiElement), MAX_USAGES);
                }
            }
        }
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        if (PsiUtils.isEventsPublish(psiElement)) {
            return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), SENDER_ICON,
                    Pass.UPDATE_ALL, null, CubaEventsLineMarkerProvider::showEventReceivers,
                    GutterIconRenderer.Alignment.LEFT);
        } else if (PsiUtils.isEventsReceiver(psiElement)) {
            return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), RECEIVER_ICON,
                    Pass.UPDATE_ALL, null, CubaEventsLineMarkerProvider::showEventSenders,
                    GutterIconRenderer.Alignment.LEFT);
        } else if (PsiUtils.isEventClass(psiElement)) {
            return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), EVENT_ICON,
                    Pass.UPDATE_ALL, null, CubaEventsLineMarkerProvider::showEventUsages,
                    GutterIconRenderer.Alignment.LEFT);
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> list, @NotNull Collection<LineMarkerInfo> collection) {
    }
}