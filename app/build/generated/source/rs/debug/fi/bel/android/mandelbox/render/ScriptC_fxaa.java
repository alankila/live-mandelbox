/*
 * Copyright (C) 2011-2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is auto-generated. DO NOT MODIFY!
 * The source Renderscript file: /Users/alankila/Open_Source/live-mandelbox/app/src/main/rs/fxaa.fs
 */

package fi.bel.android.mandelbox.render;

import android.renderscript.*;
import fi.bel.android.mandelbox.render.fxaaBitCode;

/**
 * @hide
 */
public class ScriptC_fxaa extends ScriptC {
    private static final String __rs_resource_name = "fxaa";
    // Constructor
    public  ScriptC_fxaa(RenderScript rs) {
        super(rs,
              __rs_resource_name,
              fxaaBitCode.getBitCode32(),
              fxaaBitCode.getBitCode64());
        __ALLOCATION = Element.ALLOCATION(rs);
        __SAMPLER = Element.SAMPLER(rs);
        __I32 = Element.I32(rs);
        __F32 = Element.F32(rs);
        __U8_4 = Element.U8_4(rs);
    }

    private Element __ALLOCATION;
    private Element __F32;
    private Element __I32;
    private Element __SAMPLER;
    private Element __U8_4;
    private FieldPacker __rs_fp_ALLOCATION;
    private FieldPacker __rs_fp_F32;
    private FieldPacker __rs_fp_I32;
    private FieldPacker __rs_fp_SAMPLER;
    private final static int mExportVarIdx_in = 0;
    private Allocation mExportVar_in;
    public synchronized void set_in(Allocation v) {
        setVar(mExportVarIdx_in, v);
        mExportVar_in = v;
    }

    public Allocation get_in() {
        return mExportVar_in;
    }

    public Script.FieldID getFieldID_in() {
        return createFieldID(mExportVarIdx_in, null);
    }

    private final static int mExportVarIdx_sampler = 1;
    private Sampler mExportVar_sampler;
    public synchronized void set_sampler(Sampler v) {
        setVar(mExportVarIdx_sampler, v);
        mExportVar_sampler = v;
    }

    public Sampler get_sampler() {
        return mExportVar_sampler;
    }

    public Script.FieldID getFieldID_sampler() {
        return createFieldID(mExportVarIdx_sampler, null);
    }

    private final static int mExportVarIdx_dim = 2;
    private int mExportVar_dim;
    public synchronized void set_dim(int v) {
        setVar(mExportVarIdx_dim, v);
        mExportVar_dim = v;
    }

    public int get_dim() {
        return mExportVar_dim;
    }

    public Script.FieldID getFieldID_dim() {
        return createFieldID(mExportVarIdx_dim, null);
    }

    private final static int mExportVarIdx_pixWidth = 3;
    private float mExportVar_pixWidth;
    public synchronized void set_pixWidth(float v) {
        setVar(mExportVarIdx_pixWidth, v);
        mExportVar_pixWidth = v;
    }

    public float get_pixWidth() {
        return mExportVar_pixWidth;
    }

    public Script.FieldID getFieldID_pixWidth() {
        return createFieldID(mExportVarIdx_pixWidth, null);
    }

    private final static int mExportForEachIdx_root = 0;
    public Script.KernelID getKernelID_root() {
        return createKernelID(mExportForEachIdx_root, 58, null, null);
    }

    public void forEach_root(Allocation aout) {
        forEach_root(aout, null);
    }

    public void forEach_root(Allocation aout, Script.LaunchOptions sc) {
        // check aout
        if (!aout.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        forEach(mExportForEachIdx_root, (Allocation) null, aout, null, sc);
    }

}

