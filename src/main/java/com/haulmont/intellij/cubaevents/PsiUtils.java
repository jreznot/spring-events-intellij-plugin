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

import com.intellij.lang.jvm.JvmClassKind;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;

import java.util.Objects;

import static com.haulmont.intellij.cubaevents.EventsDeclarations.*;

public class PsiUtils {

    public static PsiClass getClass(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType) psiType).resolve();
        }
        return null;
    }

    public static boolean isEventsReceiverMethod(PsiElement psiElement) {
        if (psiElement instanceof PsiMethod) {
            PsiIdentifier nameIdentifier = ((PsiMethod) psiElement).getNameIdentifier();
            if (nameIdentifier != null) {
                return isEventsReceiver(nameIdentifier);
            }
        }
        return false;
    }

    public static boolean isEventsReceiver(PsiElement psiElement) {
        if (psiElement instanceof PsiIdentifier
                && psiElement.getParent() instanceof PsiMethod) {

            PsiMethod method = (PsiMethod) psiElement.getParent();
            PsiModifierList modifierList = method.getModifierList();
            for (PsiAnnotation psiAnnotation : modifierList.getAnnotations()) {
                if (Objects.equals(psiAnnotation.getQualifiedName(), EVENTLISTENER_ANNOTATIONNAME)) {
                    return true;
                }
            }

            // it could be `ApplicationListener#onApplicationEvent` method implementation
            if (EventsDeclarations.ON_APPLICATION_EVENT_METHODNAME.equals(method.getName())
                && method.getParameterList().getParametersCount() == 1) {
                PsiClass containingClass = method.getContainingClass();

                if (containingClass == null) {
                    return false;
                }

                return isSuperClassEventListener(containingClass);
            }
        }
        return false;
    }

    public static boolean isEventsPublish(PsiElement psiElement) {
        if (psiElement instanceof PsiCallExpression) {
            PsiCallExpression callExpression = (PsiCallExpression) psiElement;
            PsiMethod method = callExpression.resolveMethod();
            if (method != null) {
                String name = method.getName();
                PsiElement parent = method.getParent();
                if (name.equals(EventsDeclarations.EVENTS_PUBLISH_METHODNAME) && parent instanceof PsiClass) {
                    PsiClass implClass = (PsiClass) parent;
                    return isEventBusClass(implClass) || isSuperClassEventBus(implClass);
                }
            }
        }
        return false;
    }

    private static boolean isEventBusClass(PsiClass psiClass) {
        return Objects.equals(psiClass.getQualifiedName(), EVENTS_CLASSNAME);
    }

    private static boolean isSuperClassEventListener(PsiClass psiClass) {
        PsiClass[] supers = psiClass.getInterfaces();
        if (supers.length == 0) {
            return false;
        }
        for (PsiClass superClass : supers) {
            if (Objects.equals(superClass.getQualifiedName(), APPLICATION_LISTENER_INTERFACENAME)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSuperClassEventBus(PsiClass psiClass) {
        PsiClass[] supers = psiClass.getSupers();
        if (supers.length == 0) {
            return false;
        }
        for (PsiClass superClass : supers) {
            if (Objects.equals(superClass.getQualifiedName(), EVENTS_CLASSNAME)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEventClass(PsiElement psiElement) {
        if (psiElement instanceof PsiIdentifier
                && psiElement.getParent() instanceof PsiClass) {

            PsiClass psiClass = (PsiClass) psiElement.getParent();
            if (psiClass.getQualifiedName() != null
                    && psiClass.getClassKind() == JvmClassKind.CLASS
                    && !psiClass.hasModifier(JvmModifier.ABSTRACT)
                    && psiClass.getQualifiedName().endsWith("Event")) {

                for (PsiClass superClass : psiClass.getSupers()) {
                    if (Objects.equals(superClass.getQualifiedName(), APPLICATION_EVENT_CLASSNAME)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}