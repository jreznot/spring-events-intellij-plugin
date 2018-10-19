/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.haulmont.intellij.cubaevents;

import com.haulmont.intellij.cubaevents.usages.ShowUsagesAction;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.lang.jvm.JvmClass;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationClassValue;
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

import static com.haulmont.intellij.cubaevents.EventsDeclarations.*;

public class SpringEventsLineMarkerProvider implements LineMarkerProvider {

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
            PsiClass eventsClass = javaPsiFacade.findClass(EVENTS_CLASSNAME, GlobalSearchScope.allScope(project));

            if (eventsClass == null) {
                // there is no events class
                return;
            }

            PsiMethod postMethod = eventsClass.findMethodsByName(EVENTS_PUBLISH_METHODNAME, false)[0];
            PsiMethod method = (PsiMethod) psiElement.getParent();

            PsiClass eventClass;
            PsiTypeElement typeElement;
            if (!method.getParameterList().isEmpty()) {
                typeElement = method.getParameterList().getParameters()[0].getTypeElement();

                if (typeElement == null) {
                    return;
                }

                eventClass = ((PsiClassType) typeElement.getType()).resolve();
            } else {
                // it could be specified in @EventListener annotation
                PsiAnnotation annotation = method.getAnnotation(EVENTLISTENER_ANNOTATIONNAME);
                if (annotation == null) {
                    return;
                }
                JvmAnnotationAttribute jvmAnnotationAttribute = annotation.getAttributes().stream()
                        .filter(attr -> "value".equals(attr.getAttributeName()) || "classes".equals(attr.getAttributeName()))
                        .findFirst()
                        .orElse(null);
                if (jvmAnnotationAttribute == null) {
                    return;
                }

                if (!(jvmAnnotationAttribute.getAttributeValue() instanceof JvmAnnotationClassValue)) {
                    return;
                }

                JvmAnnotationClassValue attributeValue = (JvmAnnotationClassValue) jvmAnnotationAttribute.getAttributeValue();
                JvmClass annotationEventClass = attributeValue.getClazz();
                if (!(annotationEventClass instanceof PsiClass)) {
                    return;
                }

                eventClass = (PsiClass) annotationEventClass;
            }

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
                    Pass.UPDATE_ALL, null, SpringEventsLineMarkerProvider::showEventReceivers,
                    GutterIconRenderer.Alignment.LEFT);
        } else if (PsiUtils.isEventsReceiver(psiElement)) {
            return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), RECEIVER_ICON,
                    Pass.UPDATE_ALL, null, SpringEventsLineMarkerProvider::showEventSenders,
                    GutterIconRenderer.Alignment.LEFT);
        } else if (PsiUtils.isEventClass(psiElement)) {
            return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), EVENT_ICON,
                    Pass.UPDATE_ALL, null, SpringEventsLineMarkerProvider::showEventUsages,
                    GutterIconRenderer.Alignment.LEFT);
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> list, @NotNull Collection<LineMarkerInfo> collection) {
    }
}