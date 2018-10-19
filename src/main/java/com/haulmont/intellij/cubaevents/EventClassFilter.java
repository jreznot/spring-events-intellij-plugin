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

public class EventClassFilter implements Predicate<Usage> {
    @Override
    public boolean test(Usage usage) {
        PsiElement element = ((UsageInfo2UsageAdapter) usage).getElement();
        if (element instanceof PsiJavaCodeReferenceElement) {
            PsiElement parent = element.getParent();

            if (parent instanceof PsiImportStatement) {
                return false;
            }

            return isNotComment(parent);
        }
        return false;
    }

    private boolean isNotComment(PsiElement psiElement) {
        PsiElement parent = psiElement;
        while (parent != null) {
            if (parent instanceof PsiComment) {
                return false;
            }
            if (parent instanceof PsiClass) {
                return true;
            }
            parent = parent.getParent();
        }
        return true;
    }
}