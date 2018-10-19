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

import com.intellij.psi.*;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;

import java.util.function.Predicate;

public class ReceiverFilter implements Predicate<Usage> {
    @Override
    public boolean test(Usage usage) {
        PsiElement element = ((UsageInfo2UsageAdapter) usage).getElement();
        if (element instanceof PsiJavaCodeReferenceElement) {
            if ((element = element.getParent()) instanceof PsiTypeElement) {
                PsiElement parent = element.getParent();

                if (parent instanceof PsiParameter) {
                    // pattern : method(SomeEvent event)
                    element = parent;

                    if ((element = element.getParent()) instanceof PsiParameterList) {
                        if ((element = element.getParent()) instanceof PsiMethod) {
                            PsiMethod method = (PsiMethod) element;
                            return PsiUtils.isEventsReceiver(method.getNameIdentifier());
                        }
                    }
                } else if (parent instanceof PsiClassObjectAccessExpression) {
                    // pattern : @EventListener(SomeEvent.class)
                    element = parent;

                    if ((element = element.getParent()) instanceof PsiNameValuePair) {
                        if ((element = element.getParent()) instanceof PsiAnnotationParameterList) {
                            if ((element = element.getParent()) instanceof PsiAnnotation) {
                                return EventsDeclarations.EVENTLISTENER_ANNOTATIONNAME.equals(
                                        ((PsiAnnotation) element).getQualifiedName()
                                );
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}