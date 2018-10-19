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

import java.util.Objects;
import java.util.function.Predicate;

public class SenderFilter implements Predicate<Usage> {

    private final PsiClass eventClass;

    public SenderFilter(PsiClass eventClass) {
        this.eventClass = eventClass;
    }

    @Override
    public boolean test(Usage usage) {
        PsiElement element = ((UsageInfo2UsageAdapter) usage).getElement();
        if (element instanceof PsiReferenceExpression) {
            if ((element = element.getParent()) instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression callExpression = (PsiMethodCallExpression) element;
                PsiType[] types = callExpression.getArgumentList().getExpressionTypes();
                for (PsiType type : types) {
                    if (Objects.equals(PsiUtils.getClass(type).getName(), eventClass.getName())) {
                        // pattern : events.post(new Event());
                        return true;
                    }
                }

                if ((element = element.getParent()) instanceof PsiExpressionStatement) {
                    if ((element = element.getParent()) instanceof PsiCodeBlock) {
                        PsiCodeBlock codeBlock = (PsiCodeBlock) element;
                        PsiStatement[] statements = codeBlock.getStatements();
                        for (PsiStatement statement : statements) {
                            if (statement instanceof PsiDeclarationStatement) {
                                PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
                                PsiElement[] elements = declarationStatement.getDeclaredElements();
                                for (PsiElement variable : elements) {
                                    if (variable instanceof PsiLocalVariable) {
                                        PsiLocalVariable localVariable = (PsiLocalVariable) variable;
                                        PsiClass psiClass = PsiUtils.getClass(localVariable.getTypeElement().getType());
                                        if (psiClass == null || eventClass == null) {
                                            return false;
                                        }

                                        if (Objects.equals(psiClass.getName(), eventClass.getName())) {
                                            // pattern :
                                            //   Event event = new Event();
                                            //   events.post(event);
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
