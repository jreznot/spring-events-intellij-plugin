package org.strangeway.springevents

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType

fun isEventsReceiverMethod(e: PsiElement): Boolean {
    return true
}

fun getClass(psiType: PsiType): PsiClass? {
    return if (psiType is PsiClassType) {
        psiType.resolve()
    } else null
}